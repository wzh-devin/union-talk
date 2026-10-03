"""消息创建、撤回和删除事件的向量索引编排。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 23:48
"""

from dataclasses import dataclass
from datetime import datetime

from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentUnitOfWork,
    AgentUnitOfWorkFactory,
)
from union_talk_agent.agent_run.application.ports.id_generator import IdGenerator
from union_talk_agent.agent_run.domain.exceptions import (
    AgentErrorCode,
    NonRetryableAgentError,
)
from union_talk_agent.event_inbox.domain.enums import InboxStatus
from union_talk_agent.event_inbox.domain.inbox_event import InboxEvent
from union_talk_agent.knowledge.application.ports.embedding_gateway import EmbeddingGateway
from union_talk_agent.knowledge.application.ports.vector_index_writer import VectorIndexWriter
from union_talk_agent.knowledge.domain.enums import VectorStatus
from union_talk_agent.knowledge.domain.message_segment import MessageSegment


@dataclass(frozen=True, slots=True)
class IndexMessageCommand:
    """Message Service 消息事件的稳定业务字段。"""

    event_id: str
    event_type: str
    message_id: int
    conversation_id: int
    sender_id: int
    sender_display_name: str
    message_type: str
    content: str
    occurred_at: datetime
    recalled: bool
    source_revision: int = 1
    source_asset_version_map: dict[int, int] | None = None
    source_message_id_list: list[int] | None = None


class MessageIndexingService:
    """以消息 ID 和修订版本维护 PostgreSQL 与 Milvus 投影。"""

    def __init__(
        self,
        *,
        unit_of_work_factory: AgentUnitOfWorkFactory,
        id_generator: IdGenerator,
        embedding_gateway: EmbeddingGateway,
        vector_index_writer: VectorIndexWriter,
        embedding_model_version: str,
        max_delivery_attempts: int,
    ) -> None:
        """
        初始化消息索引服务

        :param unit_of_work_factory: Agent 工作单元工厂
        :param id_generator: 业务 ID 生成器
        :param embedding_gateway: Embedding 生成端口
        :param vector_index_writer: 向量索引写入端口
        :param embedding_model_version: Embedding 模型版本
        :param max_delivery_attempts: 事件最大处理次数
        :return: 无返回值
        """

        self._unit_of_work_factory = unit_of_work_factory
        self._id_generator = id_generator
        self._embedding_gateway = embedding_gateway
        self._vector_index_writer = vector_index_writer
        self._embedding_model_version = embedding_model_version
        self._max_delivery_attempts = max_delivery_attempts

    async def handle(self, command: IndexMessageCommand) -> int | None:
        """
        消费消息事件并更新可检索片段

        :param command: 消息索引命令
        :return: 生效的消息片段 ID；失效事件返回空
        """

        existing_segment = await self._receive_event(command)
        if existing_segment is not None:
            return existing_segment.segment_id
        if command.recalled or command.event_type in {"MESSAGE_RECALLED", "MESSAGE_DELETED"}:
            await self._vector_index_writer.delete_message_vectors(
                command.conversation_id,
                command.message_id,
            )
            async with self._unit_of_work_factory() as unit_of_work:
                await unit_of_work.business_locks.lock("agent-message", str(command.message_id))
                await unit_of_work.message_segments.invalidate_by_message_id(command.message_id)
                await self._succeed_inbox(unit_of_work, command.event_id)
                await unit_of_work.commit()
            return None

        if command.message_type != "TEXT":
            async with self._unit_of_work_factory() as unit_of_work:
                await self._succeed_inbox(unit_of_work, command.event_id)
                await unit_of_work.commit()
            return None

        normalized_content = " ".join(command.content.split())
        if not normalized_content:
            async with self._unit_of_work_factory() as unit_of_work:
                await self._succeed_inbox(unit_of_work, command.event_id)
                await unit_of_work.commit()
            return None
        source_asset_version_map = dict(command.source_asset_version_map or {})
        async with self._unit_of_work_factory() as unit_of_work:
            source_asset_version_map.update(
                await unit_of_work.message_segments.get_asset_version_map_by_message_id_list(
                    command.source_message_id_list or []
                )
            )
            await unit_of_work.commit()
        segment = MessageSegment(
            segment_id=self._id_generator.next_id(),
            conversation_id=command.conversation_id,
            message_id=command.message_id,
            sender_id=command.sender_id,
            sender_display_name=command.sender_display_name,
            text_content=self._index_text(command.sender_display_name, normalized_content),
            occurred_at=command.occurred_at,
            source_revision=command.source_revision,
            embedding_model_version=self._embedding_model_version,
            source_asset_version_map=source_asset_version_map,
        )
        async with self._unit_of_work_factory() as unit_of_work:
            source_is_deleted = False
            for asset_file_id in sorted(segment.source_asset_version_map):
                await unit_of_work.business_locks.lock(
                    "agent-resource-lifecycle",
                    str(asset_file_id),
                )
                lifecycle = await unit_of_work.asset_lifecycles.get_by_asset_file_id(asset_file_id)
                if lifecycle is not None and str(lifecycle.status) == "DELETED":
                    source_is_deleted = True
            await unit_of_work.business_locks.lock("agent-message", str(command.message_id))
            stored = await unit_of_work.message_segments.get_by_message_id(command.message_id)
            if stored is not None:
                segment.segment_id = stored.segment_id
            await unit_of_work.message_segments.save_pending(segment)
            if source_is_deleted:
                await unit_of_work.message_segments.mark_deleted(segment.segment_id)
                await self._succeed_inbox(unit_of_work, command.event_id)
            await unit_of_work.commit()
        if source_is_deleted:
            await self._vector_index_writer.delete_message_vectors(
                command.conversation_id,
                command.message_id,
            )
            return None
        try:
            vector = (await self._embedding_gateway.encode_hybrid([segment.text_content]))[0]
            await self._vector_index_writer.replace_message_vector(segment, vector)
            segment.vector_status = VectorStatus.READY
            async with self._unit_of_work_factory() as unit_of_work:
                await unit_of_work.business_locks.lock("agent-message", str(command.message_id))
                await unit_of_work.message_segments.mark_ready(segment)
                await self._succeed_inbox(unit_of_work, command.event_id)
                await unit_of_work.commit()
        except Exception as error:
            async with self._unit_of_work_factory() as unit_of_work:
                await unit_of_work.message_segments.mark_failed(segment.segment_id)
                inbox = await unit_of_work.inbox_events.get_by_event_id(command.event_id)
                if inbox is not None:
                    inbox.fail(str(error))
                    await unit_of_work.inbox_events.update(inbox)
                await unit_of_work.commit()
            raise
        return segment.segment_id

    async def _receive_event(self, command: IndexMessageCommand) -> MessageSegment | None:
        """
        接收消息事件并判断是否已经成功处理

        :param command: 消息索引命令
        :return: 已成功事件对应片段；首次处理返回空
        """

        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock("agent-event", command.event_id)
            inbox = await unit_of_work.inbox_events.get_by_event_id(command.event_id)
            if inbox is not None and inbox.status is InboxStatus.SUCCEEDED:
                segment = await unit_of_work.message_segments.get_by_message_id(command.message_id)
                await unit_of_work.commit()
                return segment
            if inbox is not None and inbox.attempt >= self._max_delivery_attempts:
                await unit_of_work.commit()
                raise NonRetryableAgentError(
                    AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                    f"消息索引事件处理已达到{self._max_delivery_attempts}次上限",
                )
            if inbox is None:
                inbox = InboxEvent.receive(
                    command.event_id,
                    command.event_type,
                    "MESSAGE",
                    str(command.message_id),
                )
                await unit_of_work.inbox_events.insert(inbox)
            else:
                inbox.retry()
                await unit_of_work.inbox_events.update(inbox)
            await unit_of_work.commit()
        return None

    @staticmethod
    async def _succeed_inbox(unit_of_work: AgentUnitOfWork, event_id: str) -> None:
        """
        将当前 Inbox 事件标记为成功

        :param unit_of_work: 当前 Agent 工作单元
        :param event_id: 事件 ID
        :return: 无返回值
        """

        inbox_events = unit_of_work.inbox_events
        inbox = await inbox_events.get_by_event_id(event_id)
        if inbox is not None:
            inbox.succeed()
            await inbox_events.update(inbox)

    @staticmethod
    def _index_text(sender_display_name: str, content: str) -> str:
        """
        构建包含发送者语义的消息索引正文

        :param sender_display_name: 发送者展示名称
        :param content: 消息正文
        :return: 用于检索的稳定正文
        """

        sender_prefix = sender_display_name.strip()
        return f"{sender_prefix}: {content}" if sender_prefix else content
