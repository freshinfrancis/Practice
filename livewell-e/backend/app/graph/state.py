# backend/app/graph/state.py
from __future__ import annotations
from typing import TypedDict, Optional, List, Dict, Any
from ..schemas import Prisma7

class PlanItem(TypedDict, total=False):
    title: str
    description: str
    duration_minutes: int

class SessionState(TypedDict, total=False):
    # Core input
    user_id: str
    message: str

    # NLU / intent
    intent: Optional[str]

    # Daily check-in slots
    mood: Optional[str]
    energy: Optional[str]
    steps: Optional[int]

    # Check-in flow flags
    checkin_flow_active: Optional[bool]
    checkin_complete: Optional[bool]

    # Frailty (PRISMA-7)
    prisma_inputs: Optional[Prisma7]
    frailty_score: Optional[int]
    frailty_band: Optional[str]

    # PRISMA-7 multi-turn wizard
    prisma_flow_active: Optional[bool]
    prisma_cursor: Optional[int]               # 0..6
    prisma_answers: Optional[Dict[str, bool]]  # accumulates answers

    # Awaiting state: "prisma" or "slot:mood"/"slot:energy"/"slot:steps"
    awaiting: Optional[str]

    # Outputs
    plan: Optional[List[PlanItem]]
    reply: Optional[str]
    citations: Optional[List[str]]
    tone: Optional[str]
