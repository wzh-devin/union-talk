"""资源删除索引编排。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 17:38
"""

from dataclasses import dataclass
from datetime import datetime

from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentUnitOfWorkFactory,
)
from union_talk_agent.agent_run.domain.exceptions import (
    AgentErrorCode,
    NonRetryableAgentError,
)
from union_talk_agent.event_inbox.domain.enums import InboxStatus
from union_talk_agent.event_inbox.domain.inbox_event import InboxEvent
from union_talk_agent.knowledge.application.ports.vector_index_writer import (
    VectorIndexWriter,
)
from union_talk_agent.knowledge.domain.asset_lifecycle import AssetLifecycle
from union_talk_agent.knowledge.domain.enums import AssetLifecycleStatus


@dataclass(frozen=True, slots=True)
class DeleteResourceCommand:
    """File Service 资产删除事件的稳定业务字段。"""

    event_id: str
    asset_file_id: int
    conversation_id: int
    resource_version: int
    occurred_at: datetime


class ResourceDeletionService:
    """幂等失效资源元数据并删除 Milvus 全版本向量。"""

    def __init__(
        self,
        *,
        unit_of_work_factory: AgentUnitOfWorkFactory,
        vector_index_writer: VectorIndexWriter,
        max_delivery_attempts: int,
    ) -> None:
        """
        初始化 ResourceDeletionService

        :param unit_of_work_factory: Agent 工作单元工厂
        :param vector_index_writer: 资源向量索引写入端口
        :param max_delivery_attempts: 事件最大处理次数
        :return: 无返回值
        """

        self._unit_of_work_factory = unit_of_work_factory
        self._vector_index_writer = vector_index_writer
        self._max_delivery_attempts = max_delivery_attempts

    async def handle(self, command: DeleteResourceCommand) -> None:
        """
        失效资源元数据并删除全部版本向量

        :param command: 资源删除命令
        :return: 无返回值
        """

        deleted_message_id_list: list[int] = []
        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock("agent-event", command.event_id)
            await unit_of_work.business_locks.lock(
                "agent-resource-lifecycle",
                str(command.asset_file_id),
            )
            inbox = await unit_of_work.inbox_events.get_by_event_id(command.event_id)
            if inbox is not None and inbox.status is InboxStatus.SUCCEEDED:
                await unit_of_work.commit()
                return
            if inbox is not None and inbox.attempt >= self._max_delivery_attempts:
                await unit_of_work.commit()
                raise NonRetryableAgentError(
                    AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                    f"资源删除事件处理已达到{self._max_delivery_attempts}次上限",
                )
            if inbox is None:
                inbox = InboxEvent.receive(
                    command.event_id,
                    "ASSET_DELETED",
                    "ASSET",
                    str(command.asset_file_id),
                )
                await unit_of_work.inbox_events.insert(inbox)
            else:
                inbox.retry()
                await unit_of_work.inbox_events.update(inbox)
            lifecycle = await unit_of_work.asset_lifecycles.get_by_asset_file_id(
                command.asset_file_id
            )
            if lifecycle is not None and not lifecycle.accepts_deletion(
                command.resource_version,
                command.occurred_at,
            ):
                inbox.succeed()
                await unit_of_work.inbox_events.update(inbox)
                await unit_of_work.commit()
                return
            await unit_of_work.asset_lifecycles.save(
                AssetLifecycle(
                    asset_file_id=command.asset_file_id,
                    conversation_id=command.conversation_id,
                    resource_version=command.resource_version,
                    status=AssetLifecycleStatus.DELETED,
                    event_id=command.event_id,
                    occurred_at=command.occurred_at,
                )
            )
            await unit_of_work.knowledge_index.mark_deleted(command.asset_file_id)
            deleted_message_id_list = (
                await unit_of_work.message_segments.invalidate_by_asset_file_id(
                    command.asset_file_id
                )
            )
            await unit_of_work.commit()

        try:
            await self._vector_index_writer.delete_resource_vectors(command.asset_file_id)
            await self._vector_index_writer.delete_message_vector_list(
                command.conversation_id,
                deleted_message_id_list,
            )
        except Exception as error:
            async with self._unit_of_work_factory() as unit_of_work:
                await unit_of_work.business_locks.lock("agent-event", command.event_id)
                inbox = await unit_of_work.inbox_events.get_by_event_id(command.event_id)
                if inbox is not None:
                    inbox.fail(str(error))
                    await unit_of_work.inbox_events.update(inbox)
                await unit_of_work.commit()
            raise

        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock("agent-event", command.event_id)
            inbox = await unit_of_work.inbox_events.get_by_event_id(command.event_id)
            if inbox is not None:
                inbox.succeed()
                await unit_of_work.inbox_events.update(inbox)
            await unit_of_work.commit()
