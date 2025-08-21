# backend/app/graph/build.py
from __future__ import annotations

from typing import Any, Dict
from langgraph.graph import StateGraph, END

from .nodes import (
    classify_intent,
    greet_and_offer_checkin,
    handle_checkin_confirmation,
    extract_light_entities,
    ensure_slots,
    collect_slot_answer,
    start_prisma,
    continue_prisma,
    handle_post_checkin,
    handle_after_plan,
    motivate,
    maybe_compute_frailty,
    plan_routine,
    compose_reply,
    policy_router,
)

def _after_ensure_router(state: Dict[str, Any]) -> str:
    return "ask" if state.get("awaiting") else "plan"

def build_graph():
    g = StateGraph(dict)

    # Nodes
    g.add_node("classify_intent", classify_intent)

    g.add_node("greet_and_offer_checkin", greet_and_offer_checkin)
    g.add_node("handle_checkin_confirmation", handle_checkin_confirmation)
    g.add_node("handle_post_checkin", handle_post_checkin)
    g.add_node("handle_after_plan", handle_after_plan)
    g.add_node("motivate", motivate)

    g.add_node("extract_light_entities", extract_light_entities)
    g.add_node("ensure_slots", ensure_slots)
    g.add_node("collect_slot_answer", collect_slot_answer)

    g.add_node("start_prisma", start_prisma)
    g.add_node("continue_prisma", continue_prisma)

    g.add_node("maybe_compute_frailty", maybe_compute_frailty)
    g.add_node("plan_routine", plan_routine)
    g.add_node("compose_reply", compose_reply)

    # Entry
    g.set_entry_point("classify_intent")

    # Router from classify_intent
    g.add_conditional_edges(
        "classify_intent",
        policy_router,
        {
            "handle_checkin_confirmation": "handle_checkin_confirmation",
            "continue_prisma": "continue_prisma",
            "handle_post_checkin": "handle_post_checkin",
            "handle_after_plan": "handle_after_plan",
            "motivate": "motivate",
            "continue_slot": "collect_slot_answer",
            "start_prisma": "start_prisma",
            "check_slots": "extract_light_entities",
            "greet_and_offer_checkin": "greet_and_offer_checkin",
            "maybe_compute_frailty": "maybe_compute_frailty",
        },
    )

    # Conversational branches → reply
    g.add_edge("greet_and_offer_checkin", "compose_reply")
    g.add_edge("handle_checkin_confirmation", "compose_reply")
    g.add_edge("handle_post_checkin", "compose_reply")
    g.add_edge("handle_after_plan", "compose_reply")
    g.add_edge("motivate", "compose_reply")

    # Slot flow
    g.add_edge("extract_light_entities", "ensure_slots")
    g.add_conditional_edges("ensure_slots", _after_ensure_router, {"ask": "compose_reply", "plan": "maybe_compute_frailty"})
    g.add_edge("collect_slot_answer", "ensure_slots")

    # Check-in flow
    g.add_edge("start_prisma", "compose_reply")
    g.add_edge("continue_prisma", "compose_reply")

    # Planning path
    g.add_edge("maybe_compute_frailty", "plan_routine")
    g.add_edge("plan_routine", "compose_reply")

    # End
    g.add_edge("compose_reply", END)

    return g.compile()
