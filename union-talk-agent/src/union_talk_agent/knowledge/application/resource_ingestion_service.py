"""资源下载、解析、切块、向量化和状态提交编排。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:17
"""

import asyncio
from dataclasses import dataclass
from datetime import datetime

from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentUnitOfWorkFactory,
)
from union_talk_agent.agent_run.application.ports.id_generator import IdGenerator
from union_talk_agent.agent_run.domain.exceptions import (
    AgentDomainError,
    AgentErrorCode,
    NonRetryableAgentError,
    RetryableAgentError,
)
from union_talk_agent.event_inbox.application.inbox_lifecycle_service import (
    InboxLifecycleService,
)
from union_talk_agent.event_inbox.domain.enums import InboxStatus
from union_talk_agent.event_inbox.domain.inbox_event import InboxEvent
from union_talk_agent.knowledge.application.document_parser import DocumentParser
from union_talk_agent.knowledge.application.hierarchical_chunker import (
    HierarchicalChunker,
)
from union_talk_agent.knowledge.application.ports.embedding_gateway import (
    EmbeddingGateway,
)
from union_talk_agent.knowledge.application.ports.resource_content_reader import (
    ResourceContentReader,
)
from union_talk_agent.knowledge.application.ports.vector_index_writer import (
    VectorIndexWriter,
)
from union_talk_agent.knowledge.domain.asset_lifecycle import AssetLifecycle
from union_talk_agent.knowledge.domain.enums import AssetLifecycleStatus, ChunkKind, ResourceStatus
from union_talk_agent.knowledge.domain.indexed_resource import IndexedResource


@dataclass(frozen=True, slots=True)
class IndexResourceCommand:
    """File Service 内容变更事件的稳定业务字段。"""

    event_id: str
    asset_file_id: int
    conversation_id: int
    resource_version: int
    occurred_at: datetime


class ResourceIngestionService:
    """以 asset_file_id + version 为幂等键执行资源入库。"""

    def __init__(
        self,
        *,
        unit_of_work_factory: AgentUnitOfWorkFactory,
        id_generator: IdGenerator,
        inbox_lifecycle_service: InboxLifecycleService,
        content_reader: ResourceContentReader,
        document_parser: DocumentParser,
        chunker: HierarchicalChunker,
        embedding_gateway: EmbeddingGateway,
        vector_index_writer: VectorIndexWriter,
        embedding_model_version: str,
        worker_id: str,
        max_delivery_attempts: int,
    ) -> None:
        """
        初始化 ResourceIngestionService

        :param unit_of_work_factory: Agent 工作单元工厂
        :param id_generator: 业务 ID 生成器
        :param inbox_lifecycle_service: Inbox 状态流转服务
        :param content_reader: 资源内容读取端口
        :param document_parser: 文档解析器
        :param chunker: 层级切块器
        :param embedding_gateway: Embedding 生成端口
        :param vector_index_writer: 资源向量索引写入端口
        :param embedding_model_version: Embedding 模型版本
        :param worker_id: 当前 Worker 标识
        :param max_delivery_attempts: 事件最大处理次数
        :return: 无返回值
        """

        self._unit_of_work_factory = unit_of_work_factory
        self._id_generator = id_generator
        self._inbox_lifecycle_service = inbox_lifecycle_service
        self._content_reader = content_reader
        self._document_parser = document_parser
        self._chunker = chunker
        self._embedding_gateway = embedding_gateway
        self._vector_index_writer = vector_index_writer
        self._embedding_model_version = embedding_model_version
        self._worker_id = worker_id
        self._max_delivery_attempts = max_delivery_attempts

    async def handle(self, command: IndexResourceCommand) -> int:
        """
        完成一个资源版本的可重试入库并返回资源 ID

        :param command: 待执行的应用命令
        :return: 入库完成的资源 ID
        """

        processed_resource_id = await self._receive_event(command)
        if processed_resource_id is not None:
            return processed_resource_id
        try:
            content = await self._content_reader.read(
                command.asset_file_id,
                command.resource_version,
            )
            descriptor = content.descriptor
            if (
                descriptor.asset_file_id != command.asset_file_id
                or descriptor.resource_version != command.resource_version
            ):
                raise AgentDomainError(
                    AgentErrorCode.REQUEST_INVALID,
                    "File Service返回的资源版本与事件不一致",
                )
            document = await asyncio.to_thread(
                self._document_parser.parse,
                descriptor.file_name,
                descriptor.mime_type,
                content.content,
            )
        except Exception as error:
            await self._inbox_lifecycle_service.mark_failed(command.event_id, str(error))
            raise
        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock(
                "agent-resource-version",
                f"{command.asset_file_id}:{command.resource_version}",
            )
            existing = await unit_of_work.knowledge_index.get_by_asset_version(
                command.asset_file_id,
                command.resource_version,
            )
            if (
                existing is not None
                and existing.status is ResourceStatus.READY
                and existing.parser_version == self._document_parser.version
                and existing.chunker_version == self._chunker.version
                and existing.embedding_model_version == self._embedding_model_version
            ):
                await unit_of_work.commit()
                return existing.resource_id
            resource_id = (
                existing.resource_id if existing is not None else self._id_generator.next_id()
            )
            ingest_job_id = self._id_generator.next_id()
            chunk_list = self._chunker.chunk(
                resource_id,
                descriptor.conversation_id,
                document,
            )
            resource = IndexedResource(
                resource_id=resource_id,
                descriptor=descriptor,
                status=ResourceStatus.INDEXING,
                parser_version=self._document_parser.version,
                chunker_version=self._chunker.version,
                embedding_model_version=self._embedding_model_version,
                chunk_count=len(chunk_list),
                token_count=sum(chunk.token_count for chunk in chunk_list),
            )
            await unit_of_work.knowledge_index.replace_prepared_resource(
                resource,
                chunk_list,
                ingest_job_id,
                self._worker_id,
            )
            await unit_of_work.commit()

        vector_chunk_list = [
            chunk for chunk in chunk_list if chunk.chunk_kind in {ChunkKind.CHILD, ChunkKind.TABLE}
        ]
        try:
            vector_list = await self._embedding_gateway.encode_hybrid(
                [chunk.text_content for chunk in vector_chunk_list]
            )
            await self._vector_index_writer.replace_resource_vectors(
                descriptor,
                vector_chunk_list,
                vector_list,
            )
            deleted_during_ingestion = False
            async with self._unit_of_work_factory() as unit_of_work:
                await unit_of_work.business_locks.lock(
                    "agent-resource-lifecycle",
                    str(command.asset_file_id),
                )
                inbox = await unit_of_work.inbox_events.get_by_event_id(command.event_id)
                lifecycle = await unit_of_work.asset_lifecycles.get_by_asset_file_id(
                    command.asset_file_id
                )
                if lifecycle is not None and not lifecycle.accepts_content(
                    command.resource_version,
                    command.occurred_at,
                ):
                    deleted_during_ingestion = True
                    await unit_of_work.knowledge_index.mark_deleted(command.asset_file_id)
                else:
                    await unit_of_work.knowledge_index.mark_ready(
                        resource,
                        ingest_job_id,
                    )
                if inbox is not None:
                    inbox.succeed()
                    await unit_of_work.inbox_events.update(inbox)
                await unit_of_work.commit()
            if deleted_during_ingestion:
                await self._vector_index_writer.delete_resource_vectors(command.asset_file_id)
                return resource.resource_id
        except Exception as error:
            error_code = (
                str(error.code)
                if isinstance(error, AgentDomainError)
                else str(AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE)
            )
            async with self._unit_of_work_factory() as unit_of_work:
                await unit_of_work.knowledge_index.mark_failed(
                    resource.resource_id,
                    ingest_job_id,
                    error_code,
                    str(error),
                )
                inbox = await unit_of_work.inbox_events.get_by_event_id(command.event_id)
                if inbox is not None:
                    inbox.fail(str(error))
                    await unit_of_work.inbox_events.update(inbox)
                await unit_of_work.commit()
            if isinstance(error, AgentDomainError):
                raise
            raise RetryableAgentError(
                AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                "资源向量入库失败",
            ) from error
        return resource.resource_id

    async def _receive_event(self, command: IndexResourceCommand) -> int | None:
        """
        接收并解析资源索引事件

        :param command: 待执行的应用命令
        :return: 已完成事件对应的资源 ID；首次或重试事件返回空
        """

        async with self._unit_of_work_factory() as unit_of_work:
            await unit_of_work.business_locks.lock("agent-event", command.event_id)
            await unit_of_work.business_locks.lock(
                "agent-resource-lifecycle",
                str(command.asset_file_id),
            )
            inbox = await unit_of_work.inbox_events.get_by_event_id(command.event_id)
            lifecycle = await unit_of_work.asset_lifecycles.get_by_asset_file_id(
                command.asset_file_id
            )
            if lifecycle is not None and not lifecycle.accepts_content(
                command.resource_version,
                command.occurred_at,
            ):
                if inbox is None:
                    inbox = InboxEvent.receive(
                        command.event_id,
                        "ASSET_CONTENT_CHANGED",
                        "ASSET",
                        str(command.asset_file_id),
                    )
                    await unit_of_work.inbox_events.insert(inbox)
                inbox.succeed()
                await unit_of_work.inbox_events.update(inbox)
                await unit_of_work.commit()
                return 0
            if inbox is not None and inbox.status is InboxStatus.SUCCEEDED:
                resource = await unit_of_work.knowledge_index.get_by_asset_version(
                    command.asset_file_id,
                    command.resource_version,
                )
                if (
                    resource is None
                    or resource.status is not ResourceStatus.READY
                    or resource.parser_version != self._document_parser.version
                    or resource.chunker_version != self._chunker.version
                    or resource.embedding_model_version != self._embedding_model_version
                ):
                    inbox.retry()
                    await unit_of_work.inbox_events.update(inbox)
                    await unit_of_work.commit()
                    return None
                await unit_of_work.commit()
                return resource.resource_id
            if inbox is not None and inbox.attempt >= self._max_delivery_attempts:
                await unit_of_work.commit()
                raise NonRetryableAgentError(
                    AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
                    f"资源索引事件处理已达到{self._max_delivery_attempts}次上限",
                )
            if inbox is None:
                inbox = InboxEvent.receive(
                    command.event_id,
                    "ASSET_CONTENT_CHANGED",
                    "ASSET",
                    str(command.asset_file_id),
                )
                await unit_of_work.inbox_events.insert(inbox)
            else:
                inbox.retry()
                await unit_of_work.inbox_events.update(inbox)
            await unit_of_work.asset_lifecycles.save(
                AssetLifecycle(
                    asset_file_id=command.asset_file_id,
                    conversation_id=command.conversation_id,
                    resource_version=command.resource_version,
                    status=AssetLifecycleStatus.ACTIVE,
                    event_id=command.event_id,
                    occurred_at=command.occurred_at,
                )
            )
            await unit_of_work.commit()
            return None
