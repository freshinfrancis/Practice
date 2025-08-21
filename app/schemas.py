# backend/app/schemas.py
from __future__ import annotations

from typing import Any, Dict
from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    """Generic chat turn."""
    session_id: str = Field(..., min_length=1, examples=["demo-123"])
    message: str = Field(..., min_length=1, examples=["start prisma"])


class ChatResponse(BaseModel):
    """Standard response containing the assistant reply and the current graph state."""
    reply: str
    state: Dict[str, Any]


class SessionRequest(BaseModel):
    """Request that only needs a session id (e.g., to start a wizard)."""
    session_id: str = Field(..., min_length=1, examples=["demo-123"])


class PrismaAnswerRequest(SessionRequest):
    """Submit a single yes/no answer for the PRISMA-7 wizard."""
    answer: str = Field(..., min_length=1, examples=["yes"])
