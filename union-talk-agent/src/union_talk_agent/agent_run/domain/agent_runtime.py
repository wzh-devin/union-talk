"""Agent Turn、Message、Content Block 与工具执行领域模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 22:16
"""

from dataclasses import dataclass, field
from datetime import UTC, datetime

from union_talk_agent.agent_run.domain.enums import (
    AgentContentBlockStatus,
    AgentContentBlockType,
    AgentMessageStatus,
    AgentToolExecutionStatus,
    AgentTurnStatus,
    TraceVisibility,
)


@dataclass(slots=True)
class AgentRunTurn:
    """一次模型请求与其工具决策组成的 Turn。"""

    turn_id: int
    run_id: int
    turn_no: int
    status: AgentTurnStatus = AgentTurnStatus.RUNNING
    stop_reason: str | None = None
    started_at: datetime = field(default_factory=lambda: datetime.now(UTC))
    finished_at: datetime | None = None
    error_code: str | None = None
    error_message: str | None = None

    def complete(self, stop_reason: str | None) -> None:
        """
        把 Turn 标记为成功

        :param stop_reason: Provider 停止原因
        :return: 无返回值
        """

        self.status = AgentTurnStatus.SUCCEEDED
        self.stop_reason = stop_reason
        self.finished_at = datetime.now(UTC)

    def fail(self, error_code: str, error_message: str) -> None:
        """
        把 Turn 标记为失败

        :param error_code: 稳定错误码
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        self.status = AgentTurnStatus.FAILED
        self.error_code = error_code
        self.error_message = error_message[:500]
        self.finished_at = datetime.now(UTC)

    def interrupt(self) -> None:
        """
        将失去 Worker 租约的 Turn 标记为中断

        :return: 无返回值
        """

        self.status = AgentTurnStatus.INTERRUPTED
        self.stop_reason = "worker_lease_expired"
        self.finished_at = datetime.now(UTC)


@dataclass(slots=True)
class AgentRunMessage:
    """一个 Turn 内持续更新的模型消息。"""

    message_id: int
    run_id: int
    turn_id: int
    turn_no: int
    message_key: str
    role: str
    model_id: str
    status: AgentMessageStatus = AgentMessageStatus.STREAMING
    stop_reason: str | None = None
    input_tokens: int = 0
    output_tokens: int = 0
    started_at: datetime = field(default_factory=lambda: datetime.now(UTC))
    finished_at: datetime | None = None
    error_code: str | None = None
    error_message: str | None = None

    def complete(
        self,
        stop_reason: str | None,
        input_tokens: int,
        output_tokens: int,
    ) -> None:
        """
        完成模型消息并记录 Usage

        :param stop_reason: Provider 停止原因
        :param input_tokens: 输入 Token 数量
        :param output_tokens: 输出 Token 数量
        :return: 无返回值
        """

        self.status = AgentMessageStatus.COMPLETED
        self.stop_reason = stop_reason
        self.input_tokens = input_tokens
        self.output_tokens = output_tokens
        self.finished_at = datetime.now(UTC)

    def interrupt(self) -> None:
        """
        将未结束的 partial message 标记为中断

        :return: 无返回值
        """

        self.status = AgentMessageStatus.INTERRUPTED
        self.stop_reason = "worker_lease_expired"
        self.finished_at = datetime.now(UTC)


@dataclass(slots=True)
class AgentRunContentBlock:
    """模型消息中的 TEXT、THINKING 或 TOOL_CALL 内容块。"""

    block_id: int
    run_id: int
    turn_id: int
    message_id: int
    content_index: int
    block_type: AgentContentBlockType
    visibility: TraceVisibility
    status: AgentContentBlockStatus = AgentContentBlockStatus.STREAMING
    content: str = ""
    tool_call_id: str | None = None
    tool_name: str | None = None
    started_at: datetime = field(default_factory=lambda: datetime.now(UTC))
    finished_at: datetime | None = None

    def append(self, delta: str) -> None:
        """
        把 Provider 增量追加到内存中的内容块

        :param delta: Provider 内容增量
        :return: 无返回值
        """

        self.content = f"{self.content}{delta}"

    def complete(self) -> None:
        """
        完成内容块并准备持久化

        :return: 无返回值
        """

        self.status = AgentContentBlockStatus.COMPLETED
        self.finished_at = datetime.now(UTC)

    def interrupt(self) -> None:
        """
        将 Redis 中最后保存的 partial block 标记为中断

        :return: 无返回值
        """

        self.status = AgentContentBlockStatus.INTERRUPTED
        self.finished_at = datetime.now(UTC)


@dataclass(slots=True)
class AgentOpenRuntime:
    """待对账关闭的运行中 Turn、Message 与已落库内容块索引。"""

    turn: AgentRunTurn
    message: AgentRunMessage
    persisted_content_index_set: set[int] = field(default_factory=lambda: set[int]())


@dataclass(slots=True)
class AgentToolExecution:
    """模型工具调用从开始到结束的持久化生命周期。"""

    tool_execution_id: int
    run_id: int
    turn_id: int
    turn_no: int
    message_id: int
    step_id: int
    tool_call_id: str
    tool_name: str
    display_name: str
    visibility: TraceVisibility
    status: AgentToolExecutionStatus = AgentToolExecutionStatus.RUNNING
    arguments_summary: dict[str, object] = field(default_factory=lambda: dict[str, object]())
    result_summary: dict[str, object] = field(default_factory=lambda: dict[str, object]())
    started_at: datetime = field(default_factory=lambda: datetime.now(UTC))
    finished_at: datetime | None = None
    error_code: str | None = None
    error_message: str | None = None

    def succeed(self, result_summary: dict[str, object]) -> None:
        """
        完成工具执行

        :param result_summary: 面向成员的结果摘要
        :return: 无返回值
        """

        self.status = AgentToolExecutionStatus.SUCCEEDED
        self.result_summary = result_summary
        self.finished_at = datetime.now(UTC)

    def fail(self, error_code: str, error_message: str) -> None:
        """
        标记工具执行失败

        :param error_code: 稳定错误码
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        self.status = AgentToolExecutionStatus.FAILED
        self.error_code = error_code
        self.error_message = error_message[:500]
        self.finished_at = datetime.now(UTC)
