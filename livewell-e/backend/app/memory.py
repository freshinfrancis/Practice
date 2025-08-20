
from __future__ import annotations
import time
from typing import Dict, Any

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

def get(user_id: str) -> Dict[str, Any]:
    exp = _expiry.get(user_id, 0)
    if exp and exp < _now():
        _store.pop(user_id, None)
        _expiry.pop(user_id, None)
        return {}
    return dict(_store.get(user_id, {}))

def save(user_id: str, new_state: Dict[str, Any]) -> None:
    keep = {k: v for k, v in new_state.items() if k in _PERSIST_KEYS}
    prev = _store.get(user_id, {})
    prev.update(keep)
    _store[user_id] = prev
    _expiry[user_id] = _now() + _TTL_SEC

def clear(user_id: str) -> None:
    _store.pop(user_id, None)
    _expiry.pop(user_id, None)
