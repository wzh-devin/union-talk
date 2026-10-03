"""Agent Run 游标分页 HTTP 响应。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/31 15:43
"""

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

from union_talk_agent.interfaces.http.schemas.agent_run_response import AgentRunResponse


class AgentRunPageResponse(BaseModel):
    """与 Java `CursorPageResult<AgentRunResponse, Date>` 字段一致的响应。"""

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
    )

    run_list: list[AgentRunResponse] = Field(alias="list")
    has_next: bool = False
    next_cursor_value: datetime | None = None
    next_cursor_id: str | None = None
