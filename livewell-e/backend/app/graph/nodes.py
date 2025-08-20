# backend/app/graph/nodes.py

from __future__ import annotations

import re
import json
import logging
import traceback
from typing import Dict, Any, Tuple

from openai import OpenAI

from ..settings import settings
from .state import SessionState
from ..tools.frailty import prisma7_score
from ..tools.routine import generate_daily_plan

# ---- LLM client/config -------------------------------------------------------

logger = logging.getLogger("livewell")
client = OpenAI(api_key=settings.openai_api_key)
MODEL = settings.openai_model

def _call_llm(system: str, user: str, temperature: float = 0.2) -> str:
    """Guarded LLM call: never let exceptions bubble up to 500s."""
    try:
        resp = client.chat.completions.create(
            model=MODEL,
            messages=[{"role": "system", "content": system},
                      {"role": "user", "content": user}],
            temperature=temperature,
        )
        return resp.choices[0].message.content or ""
    except Exception as e:
        logger.error("LLM call failed in _call_llm: %s\n%s", e, traceback.format_exc())
        # Safe fallback text so pipeline can continue
        return "Thanks for the update. I’ll keep it simple and suggest a short, gentle plan."

# ---- PRISMA-7 flow -----------------------------------------------------------

PRISMA_ORDER = [
    ("over_85", "Are you over 85? (yes/no)"),
    ("male", "Are you male? (yes/no)"),
    ("health_problems_limit_activities", "Do health problems limit your activities? (yes/no)"),
    ("need_help_regularly", "Do you need help on a regular basis? (yes/no)"),
    ("health_problems_stay_home", "Do health problems mean you stay at home? (yes/no)"),
    ("count_on_someone_close", "Can you count on someone close to you? (yes/no)"),
    ("use_stick_walker_wheelchair", "Do you use a stick, walker, or wheelchair? (yes/no)"),
]

def _parse_yes_no(text: str) -> Tuple[bool | None, str]:
    t = text.lower().strip()
    yes = {"y", "yes", "yeah", "yep", "sure", "correct"}
    no = {"n", "no", "nope", "nah"}
    if t in yes:
        return True, ""
    if t in no:
        return False, ""
    if re.search(r"\by(es)?\b", t):
        return True, ""
    if re.search(r"\bn(o)?\b", t):
        return False, ""
    return None, "Please answer yes or no."

def start_prisma(state: SessionState) -> SessionState:
    state["prisma_flow_active"] = True
    state["prisma_cursor"] = 0
    state["prisma_answers"] = {}
    state["awaiting"] = "prisma"
    _, q = PRISMA_ORDER[0]
    state["reply"] = q
    return state

def continue_prisma(state: SessionState) -> SessionState:
    cursor = int(state.get("prisma_cursor") or 0)
    answers = dict(state.get("prisma_answers") or {})
    ans, err = _parse_yes_no(state["message"])
    if ans is None:
        state["reply"] = err or "Please answer yes or no."
        state["awaiting"] = "prisma"
        return state

    key, _ = PRISMA_ORDER[cursor]
    answers[key] = ans
    cursor += 1

    if cursor < len(PRISMA_ORDER):
        state["prisma_cursor"] = cursor
        state["prisma_answers"] = answers
        state["awaiting"] = "prisma"
        _, q = PRISMA_ORDER[cursor]
        state["reply"] = q
        return state

    # finalize
    state["prisma_flow_active"] = False
    state["awaiting"] = None
    state["prisma_cursor"] = None
    state["prisma_answers"] = answers

    payload = {
        "over_85": answers.get("over_85", False),
        "male": answers.get("male", False),
        "health_problems_limit_activities": answers.get("health_problems_limit_activities", False),
        "need_help_regularly": answers.get("need_help_regularly", False),
        "health_problems_stay_home": answers.get("health_problems_stay_home", False),
        "count_on_someone_close": answers.get("count_on_someone_close", False),
        "use_stick_walker_wheelchair": answers.get("use_stick_walker_wheelchair", False),
    }
    score, band = prisma7_score(payload)  # accepts dict or pydantic
    state["frailty_score"] = score
    state["frailty_band"] = band
    return state

def maybe_compute_frailty(state: SessionState) -> SessionState:
    """Compute frailty if full Prisma7 was provided inline with the request."""
    prisma = state.get("prisma_inputs")
    if prisma:
        score, band = prisma7_score(prisma)
        state["frailty_score"] = score
        state["frailty_band"] = band
    return state

# ---- Intent & Routing --------------------------------------------------------

def classify_intent(state: SessionState) -> SessionState:
    text = state["message"].lower().strip()
    intent = "unknown"

    if state.get("awaiting") == "prisma":
        intent = "continue_prisma"
    elif (state.get("awaiting") or "").startswith("slot:"):
        intent = "continue_slot"
    elif any(w in text for w in ["hi", "hello", "hey", "good morning", "good afternoon", "good evening"]):
        intent = "greeting"
    elif any(w in text for w in ["screen", "prisma", "frailty", "assessment", "screener", "risk score"]):
        intent = "start_prisma"
    elif any(w in text for w in ["how are you", "who are you", "what can you do"]):
        intent = "smalltalk"
    elif any(w in text for w in ["plan", "routine", "suggest", "recommend", "today", "check in", "checkin", "motivation"]):
        intent = "checkin"
    else:
        if re.search(r"\b\d{2,5}\b.*steps", text):
            intent = "checkin"

    state["intent"] = intent
    return state

def policy_router(state: SessionState) -> str:
    intent = state.get("intent", "unknown")
    if intent == "continue_prisma":
        return "continue_prisma"
    if intent == "continue_slot":
        # IMPORTANT: return the *label*, not the destination node name.
        return "continue_slot"
    if intent == "start_prisma":
        if state.get("prisma_inputs"):
            return "maybe_compute_frailty"
        return "start_prisma"
    if intent in ("greeting", "smalltalk"):
        return "smalltalk"
    # default path: attempt to fill slots, then continue
    return "check_slots"

# ---- Slot extraction and filling --------------------------------------------

def extract_light_entities(state: SessionState) -> SessionState:
    """Best-effort extraction of mood/energy/steps from user message."""
    text = state["message"]

    # steps
    m = re.search(r"(\d{2,5})\s*steps", text, re.I)
    if m:
        try:
            state["steps"] = int(m.group(1))
        except Exception:
            pass

    # mood/energy heuristics
    lowish = ["tired", "low energy", "exhausted", "fatigued", "drained"]
    highish = ["great", "good", "motivated", "energetic", "excellent", "okay", "ok"]
    tl = text.lower()
    if any(w in tl for w in lowish):
        state["mood"] = state.get("mood") or "tired"
        state["energy"] = state.get("energy") or "low"
    elif any(w in tl for w in highish):
        state["mood"] = state.get("mood") or "good"
        state["energy"] = state.get("energy") or "medium"
    return state

def ensure_slots(state: SessionState) -> SessionState:
    """Ask for one missing slot at a time."""
    needed = []
    if not state.get("mood"):
        needed.append("mood")
    if not state.get("energy"):
        needed.append("energy")
    if not state.get("steps"):
        needed.append("steps")

    if needed:
        slot = needed[0]
        state["awaiting"] = f"slot:{slot}"
        if slot == "steps":
            state["reply"] = "About how many steps did you do today? (just a number)"
        elif slot == "mood":
            state["reply"] = "How are you feeling today? (e.g., tired/good/okay)"
        else:
            state["reply"] = "How is your energy level? (low/medium/high)"
    return state

def collect_slot_answer(state: SessionState) -> SessionState:
    awaiting = state.get("awaiting", "") or ""
    if not awaiting.startswith("slot:"):
        return state

    slot = awaiting.split(":", 1)[1]
    text = state["message"].strip()

    if slot == "steps":
        m = re.search(r"\b(\d{2,5})\b", text)
        if m:
            try:
                state["steps"] = int(m.group(1))
                state["awaiting"] = None
                state["reply"] = "Got it."
                return state
            except Exception:
                pass
        state["reply"] = "Please send a number like 2500."
        return state

    if slot in ("mood", "energy"):
        val = text.lower()
        if slot == "mood":
            state["mood"] = val
        else:
            if "low" in val:
                val = "low"
            elif "high" in val:
                val = "high"
            else:
                val = "medium"
            state["energy"] = val
        state["awaiting"] = None
        state["reply"] = "Thanks."
        return state

    return state

# ---- Planning / Smalltalk / Compose -----------------------------------------

def plan_routine(state: SessionState) -> SessionState:
    band = state.get("frailty_band") or ("potential_frail" if (state.get("steps") or 0) < 3000 else "low")
    plan = generate_daily_plan(client, MODEL, band, state.get("mood"))
    state["plan"] = plan
    return state

def smalltalk(state: SessionState) -> SessionState:
    sys = (
        "You are a warm, concise assistant for older adults. "
        "Keep replies under 2 sentences. Use plain ASCII (no emojis)."
    )
    text = state["message"]
    state["reply"] = _call_llm(sys, text)
    return state

def compose_reply(state: SessionState) -> SessionState:
    # If we are waiting on a slot/PRISMA answer and a node already wrote a prompt, keep it.
    if state.get("reply") and (state.get("awaiting") or state.get("prisma_flow_active")):
        return state

    sys = (
        "You are a concise, friendly health companion for older adults. "
        "Offer encouragement, 1-2 lines max, then summarize plan items compactly. "
        "Use plain ASCII (no emojis, no smart quotes/dashes). "
        "Do not give medical advice; be informational only."
    )
    frailty = ""
    if state.get("frailty_score") is not None:
        frailty = f" Frailty (PRISMA-7): score {state['frailty_score']} ({state.get('frailty_band')})."

    plan = state.get("plan") or []
    plan_text = "; ".join(f"- {p.get('title')}: {p.get('description')}" for p in plan)

    user = (
        f"mood={state.get('mood')}, energy={state.get('energy')}, steps={state.get('steps')}."
        f"{frailty}\nToday's plan:\n{plan_text}"
    )

    state["reply"] = _call_llm(sys, user)
    return state
