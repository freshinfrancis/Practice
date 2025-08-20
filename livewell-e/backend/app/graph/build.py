# backend/app/graph/build.py
from __future__ import annotations
from langgraph.graph import StateGraph, END
from .state import SessionState
from .nodes import (
    classify_intent, policy_router,
    extract_light_entities, ensure_slots, collect_slot_answer,
    start_prisma, continue_prisma, maybe_compute_frailty,
    plan_routine, smalltalk, compose_reply,
)

def build_graph():
    g = StateGraph(SessionState)

    # Nodes
    g.add_node("classify_intent", classify_intent)
    g.add_node("extract_light_entities", extract_light_entities)
    g.add_node("ensure_slots", ensure_slots)
    g.add_node("collect_slot_answer", collect_slot_answer)
    g.add_node("start_prisma", start_prisma)
    g.add_node("continue_prisma", continue_prisma)
    g.add_node("maybe_compute_frailty", maybe_compute_frailty)
    g.add_node("plan_routine", plan_routine)
    g.add_node("smalltalk", smalltalk)
    g.add_node("compose_reply", compose_reply)

    # Entry
    g.set_entry_point("classify_intent")

    # Route after classification
    g.add_conditional_edges(
        "classify_intent",
        lambda s: policy_router(s),
        {
            "continue_prisma": "continue_prisma",
            "continue_slot": "collect_slot_answer",   # <- label -> node
            "start_prisma": "start_prisma",
            "smalltalk": "smalltalk",
            "check_slots": "extract_light_entities",
            "maybe_compute_frailty": "maybe_compute_frailty",
        },
    )

    # Smalltalk -> END
    g.add_edge("smalltalk", END)

    # Start PRISMA asks Q1 and waits for next turn
    g.add_edge("start_prisma", END)

    # Continue PRISMA: either ask next Q (END) or proceed to plan
    def prisma_next_or_plan(state: SessionState):
        if state.get("awaiting") == "prisma":
            return "ask_more"
        return "to_plan"

    g.add_conditional_edges(
        "continue_prisma",
        prisma_next_or_plan,
        {"ask_more": END, "to_plan": "plan_routine"},
    )

    # Normal path: extract -> ensure slots
    g.add_edge("extract_light_entities", "ensure_slots")

    # If ensure_slots asked something, END; else continue
    def slots_done_or_ask(state: SessionState):
        awaiting = str(state.get("awaiting") or "")
        return "asked" if awaiting.startswith("slot:") else "done"

    g.add_conditional_edges(
        "ensure_slots",
        slots_done_or_ask,
        {"asked": END, "done": "maybe_compute_frailty"},
    )

    # When collecting a slot answer: if still awaiting, END; else proceed
    def slot_collect_next(state: SessionState):
        awaiting = str(state.get("awaiting") or "")
        if awaiting.startswith("slot:"):
            return "ask_again"
        return "proceed"

    g.add_conditional_edges(
        "collect_slot_answer",
        slot_collect_next,
        {"ask_again": END, "proceed": "maybe_compute_frailty"},
    )

    # Frailty (if provided) -> plan -> compose -> END
    g.add_edge("maybe_compute_frailty", "plan_routine")
    g.add_edge("plan_routine", "compose_reply")
    g.add_edge("compose_reply", END)

    return g.compile()
