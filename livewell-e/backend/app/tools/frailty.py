from typing import Tuple, Mapping, Union
from ..schemas import Prisma7

# Accept either a Prisma7 pydantic model or a plain dict[str, bool]
InputType = Union[Prisma7, Mapping[str, bool]]

_KEYS = [
    "over_85",
    "male",
    "health_problems_limit_activities",
    "need_help_regularly",
    "health_problems_stay_home",
    "count_on_someone_close",
    "use_stick_walker_wheelchair",
]

def _get(inputs: InputType, key: str) -> bool:
    # pydantic model has attributes; dict has .get()
    if hasattr(inputs, key):
        return bool(getattr(inputs, key))
    # mapping path
    return bool(inputs.get(key, False))  # type: ignore

def prisma7_score(inputs: InputType, threshold: int = 3) -> Tuple[int, str]:
    score = sum(1 for k in _KEYS if _get(inputs, k))
    band = "potential_frail" if score >= threshold else "low"
    return score, band
