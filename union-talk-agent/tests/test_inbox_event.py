"""Inbox 事件状态流转测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 15:00
"""

from union_talk_agent.event_inbox.domain.enums import InboxStatus
from union_talk_agent.event_inbox.domain.inbox_event import InboxEvent


def test_retry_clears_previous_completion_state() -> None:
    """
    成功事件重新处理时清理旧完成时间和错误

    :return: 无返回值
    """

    inbox = InboxEvent.receive("event-1", "ASSET_CONTENT_CHANGED", "ASSET", "1")
    inbox.succeed()

    inbox.retry()

    assert inbox.status is InboxStatus.PROCESSING
    assert inbox.attempt == 2
    assert inbox.last_error is None
    assert inbox.processed_at is None


def test_failure_is_not_recorded_as_processed() -> None:
    """
    失败事件不得保留历史完成时间

    :return: 无返回值
    """

    inbox = InboxEvent.receive("event-1", "ASSET_CONTENT_CHANGED", "ASSET", "1")
    inbox.succeed()

    inbox.fail("索引失败")

    assert inbox.status is InboxStatus.FAILED
    assert inbox.last_error == "索引失败"
    assert inbox.processed_at is None
