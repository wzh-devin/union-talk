"""索引事件消费者异常分类测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 12:55
"""

from datetime import UTC, datetime
from unittest.mock import AsyncMock, MagicMock

import pytest

from union_talk_agent.agent_run.domain.exceptions import (
    AgentErrorCode,
    NonRetryableAgentError,
    RetryableAgentError,
)
from union_talk_agent.infrastructure.rabbitmq.index_event_consumer import IndexEventConsumer
from union_talk_agent.settings.rabbitmq_settings import RabbitMqSettings


def build_resource_event_body() -> bytes:
    """
    构造合法的资源内容变更事件

    :return: JSON 事件内容
    """

    return (
        "{"
        '"eventId":"asset-content-changed:101:1",'
        '"eventType":"ASSET_CONTENT_CHANGED",'
        '"schemaVersion":1,'
        '"assetFileId":101,'
        '"conversationId":201,'
        '"resourceVersion":1,'
        f'"occurredAt":"{datetime.now(UTC).isoformat()}"'
        "}"
    ).encode()


def build_deleted_resource_event_body() -> bytes:
    """
    构造合法的资产删除事件

    :return: JSON 事件内容
    """

    return (
        "{"
        '"eventId":"asset-deleted:101:1",'
        '"eventType":"ASSET_DELETED",'
        '"schemaVersion":1,'
        '"assetFileId":101,'
        '"conversationId":201,'
        '"resourceVersion":1,'
        f'"occurredAt":"{datetime.now(UTC).isoformat()}"'
        "}"
    ).encode()


@pytest.mark.asyncio
async def test_non_retryable_resource_error_is_rejected_without_requeue() -> None:
    """
    终态资源错误拒绝消息且不进入重复投递

    :return: 无返回值
    """

    settings = RabbitMqSettings(
        consumer_concurrency=1,
        retry_delay_seconds=1,
    )
    resource_service = MagicMock()
    resource_service.handle = AsyncMock(
        side_effect=NonRetryableAgentError(
            AgentErrorCode.RESOURCE_UNSUPPORTED,
            "文件类型不支持",
        )
    )
    consumer = IndexEventConsumer(
        settings,
        resource_service,
        MagicMock(),
        MagicMock(),
    )
    message = MagicMock()
    message.body = build_resource_event_body()
    message.reject = AsyncMock()
    message.nack = AsyncMock()
    message.ack = AsyncMock()

    await consumer._consume_resource(message)  # pyright: ignore[reportPrivateUsage]

    message.reject.assert_awaited_once_with(requeue=False)
    message.nack.assert_not_awaited()
    message.ack.assert_not_awaited()


@pytest.mark.asyncio
async def test_retryable_resource_error_is_requeued_after_delay(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """
    临时资源错误等待后重新投递

    :param monkeypatch: Pytest 运行时替换工具
    :return: 无返回值
    """

    sleep = AsyncMock()
    monkeypatch.setattr(
        "union_talk_agent.infrastructure.rabbitmq.index_event_consumer.asyncio.sleep",
        sleep,
    )
    settings = RabbitMqSettings(
        consumer_concurrency=1,
        retry_delay_seconds=7,
    )
    resource_service = MagicMock()
    resource_service.handle = AsyncMock(
        side_effect=RetryableAgentError(
            AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
            "Embedding服务暂时不可用",
        )
    )
    consumer = IndexEventConsumer(
        settings,
        resource_service,
        MagicMock(),
        MagicMock(),
    )
    message = MagicMock()
    message.body = build_resource_event_body()
    message.reject = AsyncMock()
    message.nack = AsyncMock()
    message.ack = AsyncMock()

    await consumer._consume_resource(message)  # pyright: ignore[reportPrivateUsage]

    sleep.assert_awaited_once_with(7)
    message.nack.assert_awaited_once_with(requeue=True)
    message.reject.assert_not_awaited()
    message.ack.assert_not_awaited()


@pytest.mark.asyncio
async def test_deleted_resource_event_is_dispatched_and_acknowledged() -> None:
    """
    合法资产删除事件调用删除服务并确认消息

    :return: 无返回值
    """

    settings = RabbitMqSettings(consumer_concurrency=1)
    deletion_service = MagicMock()
    deletion_service.handle = AsyncMock()
    consumer = IndexEventConsumer(
        settings,
        MagicMock(),
        deletion_service,
        MagicMock(),
    )
    message = MagicMock()
    message.body = build_deleted_resource_event_body()
    message.reject = AsyncMock()
    message.nack = AsyncMock()
    message.ack = AsyncMock()

    await consumer._consume_deleted_resource(message)  # pyright: ignore[reportPrivateUsage]

    deletion_service.handle.assert_awaited_once()
    message.ack.assert_awaited_once()
    message.reject.assert_not_awaited()
    message.nack.assert_not_awaited()
