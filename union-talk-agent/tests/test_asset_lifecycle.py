"""资产生命周期与删除事件测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 17:42
"""

from datetime import UTC, datetime, timedelta

from union_talk_agent.interfaces.mq.schemas.asset_content_changed_event import (
    AssetContentChangedEvent,
)
from union_talk_agent.interfaces.mq.schemas.asset_deleted_event import AssetDeletedEvent
from union_talk_agent.knowledge.domain.asset_lifecycle import AssetLifecycle
from union_talk_agent.knowledge.domain.enums import AssetLifecycleStatus


def test_asset_deleted_event_maps_stable_command() -> None:
    """
    资产删除事件完整映射为删除命令

    :return: 无返回值
    """

    event = AssetDeletedEvent.model_validate(
        {
            "eventId": "asset-deleted:101:2",
            "eventType": "ASSET_DELETED",
            "schemaVersion": 1,
            "assetFileId": 101,
            "conversationId": 201,
            "resourceVersion": 2,
            "occurredAt": "2026-08-13T17:00:00+08:00",
        }
    )
    command = event.to_command()

    assert command.asset_file_id == 101
    assert command.conversation_id == 201
    assert command.resource_version == 2


def test_asset_content_event_maps_legacy_local_time_to_utc() -> None:
    """
    既有无时区资产事件按上海时区转换为 UTC

    :return: 无返回值
    """

    event = AssetContentChangedEvent.model_validate(
        {
            "eventId": "asset-content-changed:101:2",
            "eventType": "ASSET_CONTENT_CHANGED",
            "schemaVersion": 1,
            "assetFileId": 101,
            "conversationId": 201,
            "resourceVersion": 2,
            "occurredAt": "2026-08-14 10:17:57.960",
        }
    )

    assert event.to_command().occurred_at == datetime(
        2026,
        8,
        14,
        2,
        17,
        57,
        960000,
        tzinfo=UTC,
    )


def test_asset_content_event_converts_aware_time_to_utc() -> None:
    """
    跨服务资产事件进入命令前统一转换为 UTC

    :return: 无返回值
    """

    event = AssetContentChangedEvent.model_validate(
        {
            "eventId": "asset-content-changed:101:2",
            "eventType": "ASSET_CONTENT_CHANGED",
            "schemaVersion": 1,
            "assetFileId": 101,
            "conversationId": 201,
            "resourceVersion": 2,
            "occurredAt": "2026-08-14T10:17:57.960+08:00",
        }
    )

    assert event.to_command().occurred_at == datetime(
        2026,
        8,
        14,
        2,
        17,
        57,
        960000,
        tzinfo=UTC,
    )


def test_deleted_lifecycle_rejects_same_version_content_event() -> None:
    """
    删除事实建立后不得被同版本迟到内容事件覆盖

    :return: 无返回值
    """

    deleted_at = datetime.now(UTC)
    lifecycle = AssetLifecycle(
        asset_file_id=101,
        conversation_id=201,
        resource_version=2,
        status=AssetLifecycleStatus.DELETED,
        event_id="asset-deleted:101:2",
        occurred_at=deleted_at,
    )

    assert not lifecycle.accepts_content(2, deleted_at - timedelta(minutes=1))
    assert not lifecycle.accepts_content(2, deleted_at + timedelta(minutes=1))
    assert lifecycle.accepts_content(3, deleted_at + timedelta(minutes=1))


def test_active_lifecycle_rejects_older_content_version() -> None:
    """
    当前有效版本不得被旧版本内容事件覆盖

    :return: 无返回值
    """

    occurred_at = datetime.now(UTC)
    lifecycle = AssetLifecycle(
        asset_file_id=101,
        conversation_id=201,
        resource_version=3,
        status=AssetLifecycleStatus.ACTIVE,
        event_id="asset-content-changed:101:3",
        occurred_at=occurred_at,
    )

    assert not lifecycle.accepts_content(2, occurred_at + timedelta(minutes=1))
    assert lifecycle.accepts_content(3, occurred_at)


def test_deleted_resource_message_lineage_must_be_invalidated() -> None:
    """
    删除资源后必须失效引用该资源的消息索引

    :return: 无返回值
    """

    from union_talk_agent.knowledge.domain.message_segment import MessageSegment

    segment = MessageSegment(
        segment_id=301,
        conversation_id=201,
        message_id=401,
        sender_id=0,
        sender_display_name="AI",
        text_content="文件派生回答",
        occurred_at=datetime.now(UTC),
        source_revision=1,
        embedding_model_version="BAAI/bge-m3",
        source_asset_version_map={101: 1},
    )

    assert segment.references_asset(101)
    assert not segment.references_asset(102)
