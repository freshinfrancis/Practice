# backend/app/tools/routine.py
from __future__ import annotations

"""
Resilient routine planner with gentle variation and energy scaling.

Exports:
- generate_daily_plan(slots, frailty_score, llm_suggester=None, seed=None) -> Dict[str, Any]

Behavior:
- Try optional `llm_suggester(slots, frailty_score) -> Dict`.
- Validate/normalize output.
- On error/invalid, fall back to a safe, rules-based plan that varies by a seed
  (different sessions get different variants; rotates daily) and scales intensity
  by reported energy and risk.

Plan schema:
{
  "risk": "low" | "moderate" | "high",
  "morning": [str, ...],
  "afternoon": [str, ...],
  "evening": [str, ...],
  "notes": [str, ...]
}
"""

from typing import Any, Callable, Dict, List, Optional
from datetime import datetime

Plan = Dict[str, Any]
Slots = Dict[str, Any]
LLMSuggester = Callable[[Slots, Optional[int]], Dict[str, Any]]

# ----------------------------- Utils ----------------------------------------

def _as_int(x: Any, default: int = 0, lo: Optional[int] = None, hi: Optional[int] = None) -> int:
    try:
        v = int(float(str(x).strip()))
    except Exception:
        v = default
    if lo is not None:
        v = max(lo, v)
    if hi is not None:
        v = min(hi, v)
    return v

def _as_str(x: Any, default: str = "") -> str:
    try:
        s = str(x).strip()
        return s if s else default
    except Exception:
        return default

def _risk_from_score(score: Optional[int]) -> str:
    if score is None:
        return "moderate"
    if score <= 2:
        return "low"
    if score <= 4:
        return "moderate"
    return "high"

def _validate_plan(obj: Dict[str, Any]) -> Optional[Plan]:
    try:
        required_lists = ["morning", "afternoon", "evening"]
        for k in required_lists:
            if k not in obj or not isinstance(obj[k], list) or not all(isinstance(i, str) for i in obj[k]):
                return None
        notes = obj.get("notes", [])
        if not isinstance(notes, list) or not all(isinstance(i, str) for i in notes):
            notes = []
        risk = obj.get("risk", None)
        if risk not in {"low", "moderate", "high"}:
            risk = "moderate"
        return {
            "risk": risk,
            "morning": obj["morning"],
            "afternoon": obj["afternoon"],
            "evening": obj["evening"],
            "notes": notes,
        }
    except Exception:
        return None

# ------------------------ Rules-based fallback -------------------------------

def _pick(options: List[str], variant: int) -> str:
    return options[variant % len(options)]

def _fallback_plan(slots: Slots, frailty_score: Optional[int], seed: Optional[int]) -> Plan:
    """
    Safe plan that:
    - Varies gently via `seed` (stable per-session/day).
    - Scales dosage by energy & risk (so 'easier'/'harder' actually changes the plan).
    - Always uses a concrete step target for walking.
    """
    risk = _risk_from_score(frailty_score)

    mood = _as_str(slots.get("mood"), "okay").lower()
    energy = _as_int(slots.get("energy"), default=5, lo=0, hi=10)
    steps = _as_int(slots.get("steps"), default=0, lo=0, hi=100000)

    # Variant selection: stable within a session/day; varies across sessions/days
    if seed is None:
        seed = int(datetime.utcnow().strftime("%Y%j"))  # day-based default
    variant = seed % 3

    # Movement target scales with energy & risk
    if energy <= 3:
        bump = 300
    elif energy >= 8:
        bump = 1500
    else:
        bump = 800

    if risk == "high":
        bump = int(bump * 0.6)

    walk_target = int(min(8000, max(1000, steps + bump)))
    steps_to_go = max(0, walk_target - steps)

    # Strength dosage: base by risk, adjust by energy
    base_reps = 6 if risk == "high" else (8 if risk == "moderate" else 10)
    adj = round((energy - 5) / 2.0)  # -2..+2 typically
    reps = max(4, min(14, base_reps + adj))

    # Balance hold scales with energy & risk
    base_bal = 10 if risk == "high" else (15 if risk == "moderate" else 20)
    bal = max(8, min(30, base_bal + (energy - 5)))

    hydrate = "Drink a glass of water (250 ml)"

    # Variant content (ASCII only)
    breathers = [
        "Box breathing 4-4-4-4 (5-7 min)",
        "4-7-8 breathing (5-7 min)",
        "Pursed-lip breathing (5 min)",
    ]

    # Morning walk always uses step target so energy shows up in plan
    am_moves = [
        f"Gentle walk: target ~{steps_to_go} steps",
        f"Hallway laps: aim ~{max(6, steps_to_go // 150)} easy passes",
        f"Out-and-back stroll: reach ~{steps_to_go} more steps",
    ]

    pm_strength_sets = [
        [f"Chair sit-to-stands x{reps}", f"Calf raises x{reps}"],
        [f"Wall push-ups x{reps}", f"Counter rows or band pulls x{reps}"],
        [f"Step-ups to a low step x{reps}", "Heel-to-toe walk 2x20 steps"],
    ]

    mobility_snacks = [
        "Shoulder rolls and ankle circles (2 min)",
        "Neck turns and gentle hip circles (2 min)",
        "Seated cat-cow and wrist circles (2 min)",
    ]

    evening_winddowns = [
        "Gentle stretches: hamstrings, hip flexors, chest (5 min)",
        "Progressive muscle relaxation (5-7 min)",
        "Guided mindfulness or relaxing audio (10 min)",
    ]

    morning: List[str] = [
        _pick(breathers, variant),
        _pick(am_moves, variant),
        hydrate,
    ]

    afternoon: List[str] = [
        *_pick(pm_strength_sets, variant),
        f"Balance hold near support: {bal}s/side",
        _pick(mobility_snacks, variant),
        hydrate,
    ]

    evening: List[str] = [
        _pick(evening_winddowns, variant),
        "Optional: 1-line gratitude note",
    ]

    notes: List[str] = [
        f"Mood: {mood}",
        f"Energy: {energy}/10",
        f"Steps so far: {steps}",
        f"Frailty risk (PRISMA-7): {risk}",
        "Safety: stop if pain or dizziness; keep a chair or rail nearby.",
        "If any new symptoms, consult a clinician.",
    ]

    return {
        "risk": risk,
        "morning": morning,
        "afternoon": afternoon,
        "evening": evening,
        "notes": notes,
    }

# ---------------------------- Public API ------------------------------------

def generate_daily_plan(
    slots: Slots,
    frailty_score: Optional[int],
    llm_suggester: Optional[LLMSuggester] = None,
    seed: Optional[int] = None,
) -> Plan:
    """
    Produce a daily plan. Prefer `llm_suggester` if provided, else use fallback.
    Any exception or invalid output triggers the rules-based fallback.
    `seed` (optional) nudges gentle variety in the fallback.
    """
    # Try LLM path if available
    if llm_suggester:
        try:
            proposed = llm_suggester(slots, frailty_score)
            valid = _validate_plan(proposed if isinstance(proposed, dict) else {})
            if valid:
                # Enrich with auto notes
                mood = _as_str(slots.get("mood"), "okay").lower()
                energy = _as_int(slots.get("energy"), default=5, lo=0, hi=10)
                steps = _as_int(slots.get("steps"), default=0, lo=0, hi=100000)
                risk = _risk_from_score(frailty_score)
                enrich = [
                    f"(auto) Mood: {mood}",
                    f"(auto) Energy: {energy}/10",
                    f"(auto) Steps so far: {steps}",
                    f"(auto) Frailty risk: {risk}",
                ]
                existing = set(valid.get("notes", []))
                valid["notes"].extend([n for n in enrich if n not in existing])
                if valid.get("risk") not in {"low", "moderate", "high"}:
                    valid["risk"] = risk
                return valid
        except Exception:
            pass

    # Fallback path
    return _fallback_plan(slots, frailty_score, seed)
