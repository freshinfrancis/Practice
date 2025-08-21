# backend/app/graph/nodes.py
from __future__ import annotations
from datetime import datetime
import hashlib


"""
LiveWell-E graph nodes: greeting + daily check-in (PRISMA-7 under the hood),
post-check-in follow-up, after-plan conversation, slot-filling, motivation,
and resilient planning.

Human-facing copy says "daily check-in" (never "PRISMA").
All strings use ASCII punctuation to avoid Windows console mojibake.
"""

from typing import Any, Dict, Optional
import json
import os
import re
from datetime import datetime

from ..tools.frailty import PRISMA_QUESTIONS, prisma7_score
from ..tools.routine import generate_daily_plan

# ------------------------------ Helpers -------------------------------------

# Do NOT include "ok"/"okay" in YES; treat as neutral in post flows.
YES = {"y", "yes", "yeah", "yep", "true", "sure", "go", "start", "plan"}
NO = {"n", "no", "nope", "false", "later", "not now", "skip"}

CHECKIN_RX = re.compile(
    r"\b(daily\s*check[ -]?in|check[ -]?in|checkup|check-up|todays?\s*check[ -]?in)\b",
    re.IGNORECASE,
)
MOTIVATE_RX = re.compile(r"\b(motivat(e|ion)|pep\s*talk|encourage|boost|inspire)\b", re.IGNORECASE)

def _yn(value: str) -> Optional[bool]:
    v = (value or "").strip().lower()
    if v in YES:
        return True
    if v in NO:
        return False
    return None

def _get_int(text: str) -> Optional[int]:
    if not text:
        return None
    m = re.search(r"(-?\d{1,6})", text.replace(",", ""))
    return int(m.group(1)) if m else None

def _extract_mood_from_text(text: str) -> Optional[str]:
    """Heuristic mood detection from free text (handles 'tiredd', 'okay', etc.)."""
    low = (text or "").lower()

    # tired with trailing d's (tired, tiredd, tireddd)
    if re.search(r"\btired+d*\b", low):
        return "tired"

    # fatigue family
    for w in ["fatigued", "exhausted", "weary", "sleepy"]:
        if re.search(rf"\b{w}\b", low):
            return "tired"

    # low/down/blue/stressed/anxious
    for w in ["sad", "down", "low", "depressed", "blue", "stressed", "anxious", "worried"]:
        if re.search(rf"\b{w}\b", low):
            return "low"

    # okay/ok/fine/neutral
    if re.search(r"\bok(?:ay)?\b", low) or re.search(r"\bfine\b", low) or re.search(r"\bneutral\b", low):
        return "okay"

    # good/great/happy/well/better
    for w in ["good", "great", "happy", "well", "better"]:
        if re.search(rf"\b{w}\b", low):
            return "good"

    return None

def _extract_slot_kv(text: str) -> Dict[str, Any]:
    """
    Deterministic extraction for mood/energy/steps.
    Accepts: free-text mood (e.g., "tired"), "mood: good", "energy=7",
             "steps 3200", or "3200 steps".
    """
    out: Dict[str, Any] = {}

    # Try explicit mood first
    m = re.search(r"\bmood\s*[:=]?\s*([a-zA-Z]+)", text or "", re.I)
    if m:
        out["mood"] = m.group(1).lower()
    else:
        # Fall back to free-text mood detection
        g = _extract_mood_from_text(text or "")
        if g:
            out["mood"] = g

    # energy 0..10
    m = re.search(r"\benergy\s*[:=]?\s*(\d{1,2})", text or "", re.I)
    if m:
        try:
            v = int(m.group(1))
            out["energy"] = max(0, min(10, v))
        except Exception:
            pass

    # steps: match "steps 2222" OR "2222 steps"
    m = re.search(r"\bsteps?\s*[:=]?\s*(\d{2,6})\b", text or "", re.I)
    if not m:
        m = re.search(r"\b(\d{2,6})\s+steps?\b", text or "", re.I)
    if m:
        try:
            v = int(m.group(1))
            if v >= 0:
                out["steps"] = v
        except Exception:
            pass

    return out

def _call_llm(system: str, user: str, temperature: float = 0.3, max_tokens: int = 512) -> str:
    api_key = os.getenv("OPENAI_API_KEY", "")
    model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    if not api_key:
        return "{}"
    try:
        from openai import OpenAI  # type: ignore
        client = OpenAI(api_key=api_key)
        resp = client.chat.completions.create(
            model=model,
            messages=[{"role": "system", "content": system}, {"role": "user", "content": user}],
            temperature=temperature,
            max_tokens=max_tokens,
        )
        return resp.choices[0].message.content or "{}"
    except Exception:
        return "{}"

# ------------------------------ Nodes ---------------------------------------

def classify_intent(state: Dict[str, Any]) -> Dict[str, Any]:
    txt = (state.get("messages", [])[-1].get("content") if state.get("messages") else "") or ""
    low = txt.lower()
    awaiting = (state.get("awaiting") or "")

    # Awaiting-specific flows first
    if awaiting == "confirm_checkin":
        state["label"] = "handle_checkin_confirmation"; return state
    if awaiting == "prisma":
        state["label"] = "continue_prisma"; return state
    if awaiting == "post_checkin":
        state["label"] = "handle_post_checkin"; return state
    if awaiting == "after_plan":
        state["label"] = "handle_after_plan"; return state
    if awaiting.startswith("slot:"):
        state["label"] = "continue_slot"; return state

    # Explicit intents
    if CHECKIN_RX.search(low):
        state["label"] = "start_prisma"; return state
    if MOTIVATE_RX.search(low) or low.strip() in {"motivation", "motivate", "pep", "pep talk"}:
        state["label"] = "motivate"; return state

    # Friendly opening
    if any(w in low for w in ["hi", "hello", "hey"]) or low.strip() in {"", "start", "begin"}:
        state["label"] = "greet_and_offer_checkin"; return state

    # Slots path: named, numeric-only, or free-text mood (tired/okay/etc.)
    if (
        any(k in low for k in ["mood", "energy", "steps"])
        or re.fullmatch(r"\s*\d{1,6}\s*", low)
        or _extract_mood_from_text(txt) is not None
    ):
        state["label"] = "check_slots"; return state

    state["label"] = "maybe_compute_frailty"
    return state

# ---- Greeting & check-in ----------------------------------------------------

def greet_and_offer_checkin(state: Dict[str, Any]) -> Dict[str, Any]:
    state["awaiting"] = "confirm_checkin"
    state["reply"] = (
        "Hi! Want to do your daily check-in now? It is 7 quick yes/no questions and takes about a minute."
    )
    return state

def handle_checkin_confirmation(state: Dict[str, Any]) -> Dict[str, Any]:
    user = (state.get("messages", [])[-1].get("content") if state.get("messages") else "") or ""
    yn = _yn(user)
    if yn is True:
        return start_prisma(state)
    if yn is False:
        state["awaiting"] = None
        state["reply"] = (
            "No worries. You can say \"daily check-in\" anytime. "
            "If you like, tell me your mood, energy (0-10), or steps so far."
        )
        return state
    state["awaiting"] = "confirm_checkin"
    state["reply"] = "Would you like to start your daily check-in now? (yes/no)"
    return state

# ---- Slot filling -----------------------------------------------------------

def extract_light_entities(state: Dict[str, Any]) -> Dict[str, Any]:
    user = (state.get("messages", [])[-1].get("content") if state.get("messages") else "") or ""
    # Free-text mood and explicit keys
    state.setdefault("slots", {}).update(_extract_slot_kv(user))
    return state

def ensure_slots(state: Dict[str, Any]) -> Dict[str, Any]:
    required = state.get("required_slots", ["mood", "energy", "steps"])
    slots = state.setdefault("slots", {})
    for k in required:
        if k not in slots:
            q = {
                "mood": "How's your mood today (e.g., good/okay/low)?",
                "energy": "What's your energy level 0-10?",
                "steps": "How many steps have you taken so far today?",
            }[k]
            state["awaiting"] = f"slot:{k}"
            state["reply"] = q
            return state
    state["awaiting"] = None
    return state

def collect_slot_answer(state: Dict[str, Any]) -> Dict[str, Any]:
    user = (state.get("messages", [])[-1].get("content") if state.get("messages") else "") or ""
    awaiting = (state.get("awaiting") or "")
    slots = state.setdefault("slots", {})
    required = state.get("required_slots", ["mood", "energy", "steps"])
    slot_name = awaiting.replace("slot:", "") if awaiting.startswith("slot:") else ""

    if slot_name:
        if slot_name == "mood":
            # use free-text mood detector to be forgiving
            mood = _extract_mood_from_text(user) or (user.strip().split()[0]).lower()
            slots["mood"] = mood
        elif slot_name == "energy":
            v = _get_int(user)
            if v is not None:
                slots["energy"] = max(0, min(10, v))
        elif slot_name == "steps":
            v = _get_int(user)
            if v is not None and v >= 0:
                slots["steps"] = v

    for k in required:
        if k not in slots:
            state["awaiting"] = f"slot:{k}"
            state["reply"] = {
                "mood": "Got it. What's your mood today?",
                "energy": "Thanks. What's your energy level 0-10?",
                "steps": "Thanks. How many steps have you taken so far today?",
            }[k]
            return state

    state["awaiting"] = None
    return state

def check_slots(state: Dict[str, Any]) -> Dict[str, Any]:
    extract_light_entities(state)
    ensure_slots(state)
    return state

# ---- Daily check-in (PRISMA under the hood) --------------------------------

def start_prisma(state: Dict[str, Any]) -> Dict[str, Any]:
    state["prisma_answers"] = {}
    state["prisma_index"] = 0
    question = PRISMA_QUESTIONS[0]["question"]
    state["awaiting"] = "prisma"
    state["reply"] = f"Great - daily check-in Q1: {question}"
    return state

def continue_prisma(state: Dict[str, Any]) -> Dict[str, Any]:
    user = (state.get("messages", [])[-1].get("content") if state.get("messages") else "") or ""
    i = int(state.get("prisma_index", 0))
    answers = state.setdefault("prisma_answers", {})
    if i < len(PRISMA_QUESTIONS):
        key = PRISMA_QUESTIONS[i]["key"]
        yn = _yn(user)
        if yn is not None:
            answers[key] = bool(yn)
            i += 1
            state["prisma_index"] = i
    if i < len(PRISMA_QUESTIONS):
        question = PRISMA_QUESTIONS[i]["question"]
        state["awaiting"] = "prisma"
        state["reply"] = f"Q{i+1}: {question}"
    else:
        state["awaiting"] = "post_checkin"
        state["frailty_score"] = prisma7_score(answers)
        score_txt = state.get("frailty_score")
        state["reply"] = (
            f"All done - your daily check-in score is {score_txt}. "
            "Would you like me to draft a quick plan for today, or would you prefer to log mood, energy, or steps first?"
        )
    return state

# ---- Post-check-in follow-up ------------------------------------------------

def handle_post_checkin(state: Dict[str, Any]) -> Dict[str, Any]:
    user = (state.get("messages", [])[-1].get("content") if state.get("messages") else "") or ""
    low = user.lower().strip()

    # NEW: allow starting (or restarting) the daily check-in here too
    if CHECKIN_RX.search(low) or re.search(r"\b(restart|start over|redo)\b", low):
        state["awaiting"] = None
        return start_prisma(state)

    mood = _extract_mood_from_text(user)
    if mood:
        state.setdefault("slots", {})["mood"] = mood
        state["awaiting"] = None
        return ensure_slots(state)

    if low in {"ok", "okay", "thanks", "thank you"}:
        state["awaiting"] = "post_checkin"
        state["reply"] = "Want me to put together a quick plan now (yes/no), or shall we log mood, energy, or steps?"
        return state

    if low in YES or "plan" in low or "go ahead" in low or "make it" in low or "draft" in low:
        state["awaiting"] = None
        return plan_routine(state)

    if any(k in low for k in ["mood", "energy", "steps"]) or re.fullmatch(r"\d{1,6}", low or ""):
        state["awaiting"] = None
        return check_slots(state)

    if MOTIVATE_RX.search(low) or low in {"motivation", "motivate", "pep", "pep talk"}:
        state["awaiting"] = "post_checkin"
        return motivate(state)

    state["awaiting"] = "post_checkin"
    state["reply"] = "I can draft a quick plan (say yes), or we can log mood, energy (0-10), or steps. What would you like?"
    return state

# ---- After-plan conversation ------------------------------------------------

def handle_after_plan(state: Dict[str, Any]) -> Dict[str, Any]:
    user = (state.get("messages", [])[-1].get("content") or "")
    txt = user.strip().lower()

    # NEW: allow starting a fresh daily check-in from after-plan
    if CHECKIN_RX.search(txt) or re.search(r"\b(restart|start over|redo)\b", txt):
        state["awaiting"] = None
        state["reply"] = None
        return start_prisma(state)

    mood = _extract_mood_from_text(user)
    if mood:
        state.setdefault("slots", {})["mood"] = mood
        state["awaiting"] = None
        return ensure_slots(state)

    if txt in {"ok", "okay", "thanks", "thank you"}:
        state["awaiting"] = "after_plan"
        state["reply"] = "Want tweaks (say easier/harder/swap), a pep talk (motivate), or log mood/energy/steps?"
        return state

    if MOTIVATE_RX.search(txt) or txt in {"motivation", "motivate", "pep", "pep talk"}:
        state["awaiting"] = "after_plan"
        return motivate(state)

    if "easier" in txt or "lighter" in txt:
        slots = state.setdefault("slots", {})
        energy = int(slots.get("energy", 5) or 5)
        slots["energy"] = max(0, energy - 2)
        state["reply"] = None
        return plan_routine(state)

    if "harder" in txt or "tougher" in txt:
        slots = state.setdefault("slots", {})
        energy = int(slots.get("energy", 5) or 5)
        slots["energy"] = min(10, energy + 2)
        state["reply"] = None
        return plan_routine(state)

    if "swap" in txt or "different" in txt or "variety" in txt:
        seed = int(state.get("plan_seed") or 0)
        state["plan_seed"] = seed + 1
        state["reply"] = None
        return plan_routine(state)

    if any(k in txt for k in ["mood", "energy", "steps"]) or re.fullmatch(r"\d{1,6}", txt or ""):
        state["awaiting"] = None
        return check_slots(state)

    state["awaiting"] = "after_plan"
    state["reply"] = "Say easier/harder/swap, motivate, or log mood/energy/steps."
    return state

# ---- Motivation -------------------------------------------------------------

def motivate(state: Dict[str, Any]) -> Dict[str, Any]:
    slots = state.get("slots", {})
    mood = str(slots.get("mood", "okay")).lower()
    energy = int(slots.get("energy", 5) or 5)
    steps = int(slots.get("steps", 0) or 0)
    score = state.get("frailty_score")
    risk = "low" if (score is not None and score <= 2) else ("high" if (score and score >= 5) else "moderate")

    base_bump = 500 if energy < 5 else 1000
    if risk == "high":
        base_bump = 300 if energy < 5 else 600
    walk_target = min(7000, max(1000, steps + base_bump))
    to_go = max(0, walk_target - steps)

    lines = []
    if mood in {"low", "down", "tired"} or energy <= 3:
        lines.append("Small steps count. Two minutes is enough to start.")
    else:
        lines.append("You got this. Small wins add up fast.")

    lines.append(f"Micro-goal: about {to_go} steps would meet today's gentle target.")
    lines.append("Tip: stand, roll shoulders, and walk to the kitchen and back. Then decide the next tiny step.")
    lines.append("Hydrate and breathe: 4-4-4-4 for one minute.")

    state["reply"] = "\n".join(lines)
    return state

# ---- Plan + Compose ---------------------------------------------------------

def maybe_compute_frailty(state: Dict[str, Any]) -> Dict[str, Any]:
    if state.get("frailty_score") is None and len(state.get("prisma_answers", {})) == 7:
        state["frailty_score"] = prisma7_score(state["prisma_answers"])
    return state

def _stable_seed(session_id: str | None) -> int:
    # combine session_id with day-of-year to vary daily across sessions
    day = datetime.utcnow().strftime("%Y%j")
    base = (session_id or "anon") + "|" + day
    h = hashlib.sha256(base.encode("utf-8")).hexdigest()
    return int(h[:8], 16)

def plan_routine(state: Dict[str, Any]) -> Dict[str, Any]:
    # Stable per-session/day seed for gentle variety
    seed = state.get("plan_seed")
    if seed is None:
        seed = _stable_seed(state.get("session_id"))
        state["plan_seed"] = seed

    def llm_suggester(slots: Dict[str, Any], frailty_score: Optional[int]) -> Dict[str, Any]:
        sys = "You are a gentle health coach for older adults. Return only valid, compact JSON."
        usr = (
            "Create a simple, safe day plan with keys: morning, afternoon, evening, notes. "
            "Each key should be a short list of strings. Avoid medical claims. "
            f"Slots: {json.dumps(slots)}. Frailty score: {frailty_score}."
        )
        txt = _call_llm(sys, usr, temperature=0.3)
        try:
            return json.loads(txt)
        except Exception:
            return {}

    state["plan"] = generate_daily_plan(
        state.get("slots", {}),
        state.get("frailty_score"),
        llm_suggester,
        seed=seed,
    )
    # Put convo into after-plan mode so "ok" does not re-plan
    state["awaiting"] = "after_plan"
    return state

def compose_reply(state: Dict[str, Any]) -> Dict[str, Any]:
    # If a prior node already set a reply (e.g., a question or motivation), keep it.
    if state.get("reply"):
        return state
    p = state.get("plan", {})
    def _line(key: str) -> str:
        items = p.get(key, [])
        return ", ".join(items) if isinstance(items, list) else ""
    suffix = ""
    if state.get("awaiting") == "after_plan":
        suffix = "\n\nWant tweaks (easier/harder/swap), a pep talk (motivate), or log mood/energy/steps?"
    state["reply"] = (
        "Here is your plan:\n"
        f"- Morning: {_line('morning')}\n"
        f"- Afternoon: {_line('afternoon')}\n"
        f"- Evening: {_line('evening')}\n"
        f"Notes: {', '.join(p.get('notes', [])) if isinstance(p.get('notes', []), list) else ''}"
        f"{suffix}"
    )
    return state

def policy_router(state: Dict[str, Any]) -> str:
    return (state or {}).get("label", "maybe_compute_frailty")

__all__ = [
    "classify_intent",
    "greet_and_offer_checkin",
    "handle_checkin_confirmation",
    "extract_light_entities",
    "ensure_slots",
    "collect_slot_answer",
    "check_slots",
    "start_prisma",
    "continue_prisma",
    "handle_post_checkin",
    "handle_after_plan",
    "motivate",
    "maybe_compute_frailty",
    "plan_routine",
    "compose_reply",
    "policy_router",
]
