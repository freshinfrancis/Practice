# backend/app/graph/state.py
from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List, Optional, TypedDict


# ---- Typed structures -------------------------------------------------------


class Msg(TypedDict):
    role: str           # "user" | "assistant" | "system" (optional usage)
    content: str
    ts: float           # UTC UNIX timestamp


class SessionState(TypedDict, total=False):
    # Conversation
    messages: List[Msg]
    reply: str                      # last assistant reply to return to client
    error: Optional[str]            # non-fatal error to surface

    # Slot filling
    slots: Dict[str, Any]           # e.g., {"mood":"okay","energy":5,"steps":3200}
    required_slots: List[str]       # e.g., ["mood","energy","steps"]
    awaiting: Optional[str]         # None | "prisma" | "slot:<name>"

    # PRISMA-7 wizard
    prisma_answers: Dict[str, Optional[bool]]  # keyed by question id; None if unknown
    prisma_index: int                # 0..6 pointer into PRISMA question order
    frailty_score: Optional[int]     # computed PRISMA-7 score

    # Daily plan
    plan: Dict[str, Any]             # routine / recommendations


# ---- Defaults ---------------------------------------------------------------

DEFAULT_REQUIRED_SLOTS: List[str] = ["mood", "energy", "steps"]


# ---- Constructors & helpers -------------------------------------------------


def new_session(required_slots: Optional[List[str]] = None) -> SessionState:
    """
    Create a brand-new session with clean trackers.
    """
    return {
        "messages": [],
        "slots": {},
        "required_slots": list(required_slots) if required_slots is not None else DEFAULT_REQUIRED_SLOTS.copy(),
        "awaiting": None,
        "prisma_answers": {},
        "prisma_index": 0,
        "frailty_score": None,
        "plan": {},
    }


def _now_ts() -> float:
    return datetime.utcnow().timestamp()


def add_user_message(state: SessionState, content: str) -> None:
    """
    Append a user message to the transcript.
    """
    state.setdefault("messages", []).append(
        {"role": "user", "content": content, "ts": _now_ts()}
    )


def add_assistant_message(state: SessionState, content: str) -> None:
    """
    Append an assistant message to the transcript and set it as the outgoing reply.
    """
    state.setdefault("messages", []).append(
        {"role": "assistant", "content": content, "ts": _now_ts()}
    )
    state["reply"] = content


def set_reply(state: SessionState, content: str) -> None:
    """
    Set the reply without adding a transcript entry (useful for intermediate nodes).
    """
    state["reply"] = content


def reset_reply(state: SessionState) -> None:
    """
    Remove any pending reply (use sparingly).
    """
    if "reply" in state:
        del state["reply"]


def clear_error(state: SessionState) -> None:
    """
    Clear any non-fatal error string.
    """
    if "error" in state:
        del state["error"]


def update_slot(state: SessionState, name: str, value: Any) -> None:
    """
    Upsert a single slot value.
    """
    state.setdefault("slots", {})[name] = value


__all__ = [
    "Msg",
    "SessionState",
    "DEFAULT_REQUIRED_SLOTS",
    "new_session",
    "add_user_message",
    "add_assistant_message",
    "set_reply",
    "reset_reply",
    "clear_error",
    "update_slot",
]
