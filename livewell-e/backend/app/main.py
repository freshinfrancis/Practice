from __future__ import annotations

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from dotenv import load_dotenv

from .graph.build import build_graph
from .schemas import Prisma7
from .tools.frailty import prisma7_score
from . import memory

import logging
logging.basicConfig(level=logging.INFO)

# Load environment variables (e.g., OPENAI_API_KEY, OPENAI_MODEL)
load_dotenv()

app = FastAPI(
    title="LiveWell-E Agent API",
    version="0.3.0",
    description="FastAPI + LangGraph backend for the LiveWell-E assistant.",
)

# Basic CORS for local/dev; restrict origins in production.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],        # TODO: change to your app origins in prod
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Compile the LangGraph once at startup
graph = build_graph()


# ====== Request/Response Schemas ======

class ChatRequest(BaseModel):
    user_id: str
    message: str
    prisma7: Prisma7 | None = None  # optional inline PRISMA-7 answers


class ChatResponse(BaseModel):
    reply: str
    plan: list | None = None
    frailty: dict | None = None
    citations: list[str] | None = None


# ====== Health & Root ======

@app.get("/")
def root():
    return {"service": "LiveWell-E Agent API", "status": "online"}


@app.get("/health")
def health():
    return {"status": "ok"}


# ====== Assessments ======

@app.post("/assessments/prisma7")
def assess_prisma7(payload: Prisma7):
    """
    Standalone PRISMA-7 scoring endpoint.
    Usage: POST the 7 yes/no answers; returns {score, band}.
    """
    score, band = prisma7_score(payload)
    return {"score": score, "band": band}


# ====== Chat ======

@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    """
    Main conversational endpoint.
    - Accepts {user_id, message} and optional {prisma7} answers.
    - Restores short-term memory for this user, runs the LangGraph pipeline,
      persists dialog-relevant fields, and returns the composed reply plus
      optional plan and frailty summary.
    """
    if not req.message or not req.message.strip():
        raise HTTPException(status_code=400, detail="Empty message")

    # Restore short-term state for this user (dialog context)
    persisted = memory.get(req.user_id)

    # Seed this turn's state; graph nodes will progressively enrich it
    state = {
        **persisted,
        "user_id": req.user_id,
        "message": req.message,
    }
    if req.prisma7 is not None:
        state["prisma_inputs"] = req.prisma7

    # Run the graph
    result = graph.invoke(state)

    # Persist dialog-relevant bits for the next turn
    memory.save(req.user_id, result)

    return ChatResponse(
        reply=result.get("reply", "Hi!"),
        plan=result.get("plan"),
        frailty=(
            {"score": result.get("frailty_score"), "band": result.get("frailty_band")}
            if result.get("frailty_score") is not None
            else None
        ),
        citations=result.get("citations"),
    )
