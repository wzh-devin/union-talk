"""Inbox 事件模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from dataclasses import dataclass
from datetime import UTC, datetime

from union_talk_agent.event_inbox.domain.enums import InboxStatus


@dataclass(slots=True)
class InboxEvent:
    """RabbitMQ 至少一次投递的幂等记录。"""

    event_id: str
    event_type: str
    aggregate_type: str
    aggregate_id: str
    status: InboxStatus = InboxStatus.PROCESSING
    attempt: int = 1
    last_error: str | None = None
    received_at: datetime | None = None
    processed_at: datetime | None = None

    @classmethod
    def receive(
        cls,
        event_id: str,
        event_type: str,
        aggregate_type: str,
        aggregate_id: str,
    ) -> "InboxEvent":
        """
        创建首次接收记录

        :param event_id: 事件 ID
        :param event_type: Agent 实时事件类型
        :param aggregate_type: 事件聚合类型
        :param aggregate_id: 事件聚合标识
        :return: 处理中的 Inbox 记录
        """

        return cls(
            event_id=event_id,
            event_type=event_type,
            aggregate_type=aggregate_type,
            aggregate_id=aggregate_id,
            received_at=datetime.now(UTC),
        )

    def retry(self) -> None:
        """
        开始下一次处理

        :return: 无返回值
        """

        self.status = InboxStatus.PROCESSING
        self.attempt += 1
        self.last_error = None
        self.processed_at = None

    def succeed(self) -> None:
        """
        标记消费成功

        :return: 无返回值
        """

        self.status = InboxStatus.SUCCEEDED
        self.processed_at = datetime.now(UTC)

    def fail(self, error_message: str) -> None:
        """
        标记消费失败

        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        self.status = InboxStatus.FAILED
        self.last_error = error_message[:500]
        self.processed_at = None
