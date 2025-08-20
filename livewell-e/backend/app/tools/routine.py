from typing import List, Dict, Any
from openai import OpenAI
import json
import logging, traceback
logger = logging.getLogger("livewell")

FALLBACK_PLAN = [
    {"title":"Easy Walk", "description":"10-15 min flat walk.", "duration_minutes":15},
    {"title":"Hydration", "description":"Drink a glass of water now.", "duration_minutes":1},
]

def generate_daily_plan(
    client: OpenAI, model: str, frailty_band: str, mood: str | None
) -> List[Dict[str, Any]]:
    sys = (
        "You are a brief, safety-aware coach for older adults. "
        "Return 2-3 practical items in pure JSON array (no extra text). "
        "Use plain ASCII; fields: title, description, duration_minutes."
    )
    user = f"Frailty band: {frailty_band or 'unknown'}; Mood: {mood or 'neutral'}."

    try:
        resp = client.chat.completions.create(
            model=model,
            messages=[{"role":"system","content":sys},{"role":"user","content":user}],
            temperature=0.4,
        )
        text = resp.choices[0].message.content or "[]"
    except Exception as e:
        logger.error("LLM call failed in generate_daily_plan: %s\n%s", e, traceback.format_exc())
        return FALLBACK_PLAN

    try:
        plan = json.loads(text)
        if isinstance(plan, dict):
            plan = plan.get("items", [])
        if not isinstance(plan, list):
            raise ValueError("plan is not list")
        # light normalize
        norm = []
        for p in plan[:3]:
            if not isinstance(p, dict): continue
            norm.append({
                "title": str(p.get("title",""))[:80],
                "description": str(p.get("description",""))[:200],
                "duration_minutes": int(p.get("duration_minutes") or 10),
            })
        return norm if norm else FALLBACK_PLAN
    except Exception as e:
        logger.warning("Could not parse plan JSON, using fallback: %s", e)
        return FALLBACK_PLAN
