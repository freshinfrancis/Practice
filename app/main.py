# backend/app/main.py
from __future__ import annotations

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .schemas import ChatRequest, ChatResponse, SessionRequest, PrismaAnswerRequest
from .memory import get_session, set_session
from .graph.state import add_user_message, add_assistant_message, reset_reply
from .graph.build import build_graph


app = FastAPI(title="LiveWell-E", version="1.0.0")

# Open CORS for local testing (tighten in production)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Build the LangGraph once per process
GRAPH = build_graph()


@app.get("/health")
def health():
    return {"ok": True, "service": "livewell-e"}


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    """
    Generic chat endpoint that runs the LangGraph over the session state.
    Clears any stale reply before invoking the graph so each turn composes fresh output.
    Also writes the session_id into state so the planner can seed variety per session/day.
    """
    st = get_session(req.session_id)
    # Make session_id available inside the graph for stable seeding/analytics
    st["session_id"] = req.session_id

    add_user_message(st, req.message)
    reset_reply(st)

    # Run the graph (may return a new dict/state)
    new_state = GRAPH.invoke(st)

    # Ensure reply is recorded in transcript
    reply = new_state.get("reply") or "Okay."
    add_assistant_message(new_state, reply)

    # Persist the updated state (after assistant message)
    set_session(req.session_id, new_state)

    return {"reply": reply, "state": new_state}


# --- Daily check-in (PRISMA-7 under the hood) helper endpoints ---

@app.post("/assessments/prisma7/start", response_model=ChatResponse)
def prisma_start(req: SessionRequest):
    """
    Kick off the daily check-in by sending a natural-language cue through the graph.
    """
    st = get_session(req.session_id)
    st["session_id"] = req.session_id

    add_user_message(st, "daily check-in")
    reset_reply(st)

    new_state = GRAPH.invoke(st)
    reply = new_state.get("reply") or ""
    add_assistant_message(new_state, reply)
    set_session(req.session_id, new_state)

    return {"reply": reply, "state": new_state}


@app.post("/assessments/prisma7/answer", response_model=ChatResponse)
def prisma_answer(req: PrismaAnswerRequest):
    """
    Advance the daily check-in by submitting a yes/no answer.
    """
    st = get_session(req.session_id)
    st["session_id"] = req.session_id

    add_user_message(st, req.answer)
    reset_reply(st)

    new_state = GRAPH.invoke(st)
    reply = new_state.get("reply") or ""
    add_assistant_message(new_state, reply)
    set_session(req.session_id, new_state)

    return {"reply": reply, "state": new_state}


# --- Optional: greet first so the agent speaks without a user prompt ---

@app.post("/session/welcome", response_model=ChatResponse)
def session_welcome(req: SessionRequest):
    """
    Let the agent greet and offer the daily check-in proactively.
    """
    st = get_session(req.session_id)
    st["session_id"] = req.session_id

    reply = (
        "Hi! Ready for your daily check-in? It's 7 quick yes/no questions and takes about a minute. "
        'Say "yes" to begin, or share your mood/energy/steps.'
    )
    st["awaiting"] = "confirm_checkin"
    add_assistant_message(st, reply)
    set_session(req.session_id, st)
    return {"reply": reply, "state": st}
