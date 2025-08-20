from pydantic import BaseModel

class Prisma7(BaseModel):
    # 1
    over_85: bool
    # 2
    male: bool
    # 3
    health_problems_limit_activities: bool
    # 4
    need_help_regularly: bool
    # 5
    health_problems_stay_home: bool
    # 6
    count_on_someone_close: bool
    # 7
    use_stick_walker_wheelchair: bool
