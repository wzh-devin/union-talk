"""Agent Run 游标分页结果。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/31 15:28
"""

from dataclasses import dataclass
from datetime import datetime

from union_talk_agent.agent_run.domain.agent_run import AgentRun


@dataclass(frozen=True, slots=True)
class AgentRunCursorPage:
    """按排队时间和 Run ID 定位的 Agent Run 游标页。"""

    run_list: list[AgentRun]
    has_next: bool
    next_cursor_value: datetime | None
    next_cursor_id: int | None

    @classmethod
    def from_candidate_list(
        cls,
        candidate_list: list[AgentRun],
        page_size: int,
    ) -> "AgentRunCursorPage":
        """
        从多查一条的候选列表构造游标页

        :param candidate_list: 已按排队时间和 Run ID 倒序排列的候选列表
        :param page_size: 当前页展示数量
        :return: 包含下一页双游标的 Agent Run 游标页
        """

        run_list = candidate_list[:page_size]
        cursor_run = run_list[-1] if run_list else None
        has_next = (
            len(candidate_list) > page_size
            and cursor_run is not None
            and cursor_run.queued_at is not None
        )
        return cls(
            run_list=run_list,
            has_next=has_next,
            next_cursor_value=cursor_run.queued_at if has_next and cursor_run else None,
            next_cursor_id=cursor_run.run_id if has_next and cursor_run else None,
        )
