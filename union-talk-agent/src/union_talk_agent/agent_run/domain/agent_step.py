"""Agent 结构化执行步骤。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from dataclasses import dataclass
from datetime import UTC, datetime

from union_talk_agent.agent_run.domain.enums import (
    AgentStepStatus,
    AgentStepType,
    TraceVisibility,
)


@dataclass(slots=True)
class AgentStep:
    """不包含隐藏思维链的产品级执行步骤。"""

    step_id: int
    run_id: int
    sequence_no: int
    step_type: AgentStepType
    step_code: str
    display_name: str
    visibility: TraceVisibility
    status: AgentStepStatus = AgentStepStatus.PENDING
    started_at: datetime | None = None
    finished_at: datetime | None = None
    error_code: str | None = None
    error_message: str | None = None

    def start(self) -> None:
        """
        将步骤标记为运行中

        :return: 无返回值
        """

        self.status = AgentStepStatus.RUNNING
        self.started_at = datetime.now(UTC)

    def complete(self) -> None:
        """
        将步骤标记为成功

        :return: 无返回值
        """

        self.status = AgentStepStatus.SUCCEEDED
        self.finished_at = datetime.now(UTC)

    def fail(self, error_code: str, error_message: str) -> None:
        """
        将步骤标记为失败

        :param error_code: 脱敏错误码
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        self.status = AgentStepStatus.FAILED
        self.error_code = error_code
        self.error_message = error_message
        self.finished_at = datetime.now(UTC)
