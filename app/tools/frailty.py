# backend/app/tools/frailty.py
from __future__ import annotations

"""
Deterministic PRISMA-7 frailty scoring utilities.

- 7 yes/no questions scored as 1 for "risk" and 0 otherwise.
- Question 6 ("someone_close") is reverse-coded: NO = 1 (risk), YES = 0.
- Returns an integer score 0..7 when all answers are present, else None.

Also includes small helpers to run a wizard flow:
- prisma7_next_unanswered(...)  -> (key, question) or None
- prisma7_set_answer(...)       -> normalize and set a single answer
- prisma7_is_high_risk(score)   -> threshold check (>=3 by convention)
"""

from typing import Dict, Optional, Any, Tuple

# ---------------------------------------------------------------------------
# Questions (order matters). `reverse=True` means NO counts as 1 instead of YES.
# ---------------------------------------------------------------------------

PRISMA_QUESTIONS = [
    {
        "key": "over_85",
        "question": "Are you over 85? (yes/no)",
        "reverse": False,
    },
    {
        "key": "male",
        "question": "Are you male? (yes/no)",
        "reverse": False,
    },
    {
        "key": "limit_activities",
        "question": "Do health problems limit your activities? (yes/no)",
        "reverse": False,
    },
    {
        "key": "need_help_regularly",
        "question": "Do you need help on a regular basis? (yes/no)",
        "reverse": False,
    },
    {
        "key": "stay_home",
        "question": "Do health problems force you to stay at home? (yes/no)",
        "reverse": False,
    },
    {
        # Reverse-coded: having someone to count on lowers risk
        "key": "someone_close",
        "question": "In case of need, can you count on someone close to you? (yes/no)",
        "reverse": True,  # NO -> 1 point (risk), YES -> 0
    },
    {
        "key": "use_aid",
        "question": "Do you regularly use a cane, walker, or wheelchair? (yes/no)",
        "reverse": False,
    },
]

PRISMA_ORDER = [q["key"] for q in PRISMA_QUESTIONS]

PRISMA_HIGH_RISK_THRESHOLD = 3  # >=3 suggests higher frailty risk


# ---------------------------------------------------------------------------
# Normalization
# ---------------------------------------------------------------------------

def _normalize_bool(value: Any) -> Optional[bool]:
    """
    Accepts: bool, 'yes'/'no', 'y'/'n', 'true'/'false', '1'/'0'.
    Returns True/False, or None if it can't be parsed.
    """
    if isinstance(value, bool):
        return value
    if value is None:
        return None
    s = str(value).strip().lower()
    if s in {"y", "yes", "true", "1"}:
        return True
    if s in {"n", "no", "false", "0"}:
        return False
    return None


# ---------------------------------------------------------------------------
# Core scoring
# ---------------------------------------------------------------------------

def prisma7_score(answers: Dict[str, Optional[Any]]) -> Optional[int]:
    """
    Compute PRISMA-7 score.
    - `answers` maps each key in PRISMA_ORDER to a boolean-like value.
    - Returns None if any answer is missing/invalid.
    """
    # Ensure all 7 are present and parseable
    normalized: Dict[str, Optional[bool]] = {}
    for q in PRISMA_QUESTIONS:
        k = q["key"]
        if k not in answers:
            return None
        normalized[k] = _normalize_bool(answers.get(k))
        if normalized[k] is None:
            return None

    # Score with reverse coding for "someone_close"
    score = 0
    for q in PRISMA_QUESTIONS:
        k, reverse = q["key"], q["reverse"]
        v = bool(normalized[k])
        if reverse:
            score += int(not v)   # NO -> 1 point
        else:
            score += int(v)       # YES -> 1 point
    return score


def prisma7_is_high_risk(score: Optional[int]) -> Optional[bool]:
    """
    Returns True if score >= threshold, False if below, None if score is None.
    """
    if score is None:
        return None
    return score >= PRISMA_HIGH_RISK_THRESHOLD


# ---------------------------------------------------------------------------
# Wizard helpers
# ---------------------------------------------------------------------------

def prisma7_next_unanswered(answers: Dict[str, Optional[Any]]) -> Optional[Tuple[str, str]]:
    """
    Returns (key, question) for the next unanswered item, or None if all answered.
    A key is considered answered only if it normalizes to True/False.
    """
    for q in PRISMA_QUESTIONS:
        k = q["key"]
        v = answers.get(k, None)
        if _normalize_bool(v) is None:
            return k, q["question"]
    return None


def prisma7_set_answer(answers: Dict[str, Optional[Any]], key: str, value: Any) -> None:
    """
    Normalizes and sets a single PRISMA answer in-place. If key is unknown, no-op.
    """
    if key not in PRISMA_ORDER:
        return
    answers[key] = _normalize_bool(value)


__all__ = [
    "PRISMA_QUESTIONS",
    "PRISMA_ORDER",
    "PRISMA_HIGH_RISK_THRESHOLD",
    "prisma7_score",
    "prisma7_is_high_risk",
    "prisma7_next_unanswered",
    "prisma7_set_answer",
]
