# backend/app/memory.py
from __future__ import annotations

"""
In-memory session store for LiveWell-E.

- get_session / set_session: full LangGraph SessionState per session_id.
- reset_session / list_sessions: test & debug helpers.
- Optional TTL snapshot (get/save/clear): lightweight persisted subset of fields
  you might want to read elsewhere without the whole SessionState.

Note: This storage is per-process. Use a DB/Redis in production.
"""

from typing import Dict, Any
import time

from .graph.state import SessionState, new_session


# ---------------------------------------------------------------------------
# Full SessionState store
# ---------------------------------------------------------------------------

_SESSIONS: Dict[str, SessionState] = {}


def get_session(session_id: str) -> SessionState:
    """Fetch (or lazily create) the session state for a session_id."""
    st = _SESSIONS.get(session_id)
    if st is None:
        st = new_session()
        _SESSIONS[session_id] = st
    return st


def set_session(session_id: str, state: SessionState) -> SessionState:
    """
    Persist/replace the session state after a graph turn.
    Also updates the TTL snapshot (optional) with a compact subset.
    """
    _SESSIONS[session_id] = state
    # Keep the TTL snapshot in sync (safe no-op if you never read it)
    save(session_id, _select_persist_subset(state))
    return state


def reset_session(session_id: str) -> SessionState:
    """Clear and recreate a session (useful for tests)."""
    st = new_session()
    _SESSIONS[session_id] = st
    # Reset TTL snapshot too
    clear(session_id)
    return st


def list_sessions() -> Dict[str, SessionState]:
    """Return the full in-memory session map (debug only)."""
    return _SESSIONS


# ---------------------------------------------------------------------------
# TTL snapshot (optional): keep only dialog-relevant bits for quick access
# ---------------------------------------------------------------------------

_TTL_SEC = 60 * 60  # 1 hour
_store: Dict[str, Dict[str, Any]] = {}
_expiry: Dict[str, float] = {}

# Only persist the dialog-relevant bits between turns
_PERSIST_KEYS = {
    "mood", "energy", "steps",
    "prisma_flow_active", "prisma_cursor", "prisma_answers", "awaiting",
    "frailty_score", "frailty_band",
}


def _now() -> float:
    return time.time()


def _select_persist_subset(state: Dict[str, Any]) -> Dict[str, Any]:
    """
    Build a compact dict from the full SessionState using _PERSIST_KEYS.
    - Pull mood/energy/steps from state['slots'] if present.
    - Copy a few top-level fields (awaiting, prisma_answers, frailty_score, ...).
    """
    out: Dict[str, Any] = {}

    # slots → flatten mood/energy/steps
    slots = state.get("slots") or {}
    for k in ("mood", "energy", "steps"):
        if k in slots:
            out[k] = slots[k]

    # copy selected top-level fields if present
    for k in ("prisma_flow_active", "prisma_cursor", "prisma_answers",
              "awaiting", "frailty_score", "frailty_band"):
        if k in state:
            out[k] = state[k]

    # keep only whitelisted keys
    return {k: v for k, v in out.items() if k in _PERSIST_KEYS}


def get(user_id: str) -> Dict[str, Any]:
    """Read the TTL snapshot for a user_id/session_id (clears if expired)."""
    exp = _expiry.get(user_id, 0.0)
    if exp and exp < _now():
        _store.pop(user_id, None)
        _expiry.pop(user_id, None)
        return {}
    return dict(_store.get(user_id, {}))


def save(user_id: str, new_state: Dict[str, Any]) -> None:
    """Write (merge) the TTL snapshot for a user_id/session_id."""
    keep = {k: v for k, v in new_state.items() if k in _PERSIST_KEYS}
    prev = _store.get(user_id, {})
    prev.update(keep)
    _store[user_id] = prev
    _expiry[user_id] = _now() + _TTL_SEC


def clear(user_id: str) -> None:
    """Delete the TTL snapshot for a user_id/session_id."""
    _store.pop(user_id, None)
    _expiry.pop(user_id, None)
