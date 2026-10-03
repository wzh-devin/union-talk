"""Index Worker 显式依赖容器。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 00:05
"""

import os
import socket
import uuid
from dataclasses import dataclass

import httpx
from pymilvus import MilvusClient

from union_talk_agent.event_inbox.application.inbox_lifecycle_service import (
    InboxLifecycleService,
)
from union_talk_agent.infrastructure.document.tesseract_pdf_ocr import TesseractPdfOcr
from union_talk_agent.infrastructure.embedding.openai_embedding_client import (
    OpenAiEmbeddingClient,
)
from union_talk_agent.infrastructure.grpc.file_grpc_resource_content_reader import (
    FileGrpcResourceContentReader,
)
from union_talk_agent.infrastructure.id_generation.snowflake_id_generator import (
    SnowflakeIdGenerator,
)
from union_talk_agent.infrastructure.milvus.collection_manager import (
    MilvusCollectionManager,
)
from union_talk_agent.infrastructure.milvus.pymilvus_client_adapter import (
    PyMilvusClientAdapter,
)
from union_talk_agent.infrastructure.milvus.resource_vector_index import (
    MilvusResourceVectorIndex,
)
from union_talk_agent.infrastructure.postgres.database import PostgresDatabase
from union_talk_agent.infrastructure.postgres.unit_of_work import PostgresUnitOfWorkFactory
from union_talk_agent.infrastructure.rabbitmq.index_event_consumer import IndexEventConsumer
from union_talk_agent.knowledge.application.document_parser import DocumentParser
from union_talk_agent.knowledge.application.hierarchical_chunker import HierarchicalChunker
from union_talk_agent.knowledge.application.message_indexing_service import (
    MessageIndexingService,
)
from union_talk_agent.knowledge.application.resource_deletion_service import (
    ResourceDeletionService,
)
from union_talk_agent.knowledge.application.resource_ingestion_service import (
    ResourceIngestionService,
)
from union_talk_agent.settings import Settings


@dataclass(slots=True)
class IndexContainer:
    """持有 Index Worker 单进程共享依赖。"""

    database: PostgresDatabase
    http_client: httpx.AsyncClient
    content_reader: FileGrpcResourceContentReader
    milvus_manager: MilvusCollectionManager
    consumer: IndexEventConsumer

    async def close(self) -> None:
        """
        按依赖逆序关闭连接池

        :return: 无返回值
        """

        await self.consumer.close()
        await self.milvus_manager.close()
        await self.content_reader.close()
        await self.http_client.aclose()
        await self.database.close()


def build_index_container(settings: Settings) -> IndexContainer:
    """
    构造 Index Worker 依赖图

    :param settings: Agent 应用配置
    :return: Index Worker 依赖容器
    """

    if not settings.milvus.enabled:
        raise RuntimeError("Index Worker要求启用Milvus")
    database = PostgresDatabase(settings.postgres)
    unit_of_work_factory = PostgresUnitOfWorkFactory(database.session_factory)
    id_generator = SnowflakeIdGenerator(
        settings.app.id_worker_id,
        settings.app.id_datacenter_id,
    )
    http_client = httpx.AsyncClient()
    content_reader = FileGrpcResourceContentReader(
        settings.grpc.file_target,
        settings.grpc.deadline_seconds,
        http_client,
    )
    embedding_client = OpenAiEmbeddingClient(
        http_client,
        settings.embedding.api_base,
        settings.embedding.api_key,
        settings.embedding.model,
        settings.embedding.dimension,
        settings.embedding.timeout_seconds,
    )
    milvus_client = PyMilvusClientAdapter(
        MilvusClient(uri=settings.milvus.uri, token=settings.milvus.token)
    )
    milvus_manager = MilvusCollectionManager(
        milvus_client,
        settings.milvus.collection_name,
        settings.embedding.dimension,
    )
    vector_index = MilvusResourceVectorIndex(
        milvus_client,
        settings.milvus.collection_name,
    )
    worker_id = f"{socket.gethostname()}:{os.getpid()}:{uuid.uuid4().hex[:8]}"
    resource_service = ResourceIngestionService(
        unit_of_work_factory=unit_of_work_factory,
        id_generator=id_generator,
        inbox_lifecycle_service=InboxLifecycleService(unit_of_work_factory),
        content_reader=content_reader,
        document_parser=DocumentParser(
            TesseractPdfOcr(
                enabled=settings.ocr.enabled,
                languages=settings.ocr.languages,
                dpi=settings.ocr.dpi,
                page_timeout_seconds=settings.ocr.page_timeout_seconds,
                max_pages=settings.ocr.max_pages,
            )
        ),
        chunker=HierarchicalChunker(id_generator),
        embedding_gateway=embedding_client,
        vector_index_writer=vector_index,
        embedding_model_version=settings.embedding.model,
        worker_id=worker_id,
        max_delivery_attempts=settings.rabbitmq.max_delivery_attempts,
    )
    message_service = MessageIndexingService(
        unit_of_work_factory=unit_of_work_factory,
        id_generator=id_generator,
        embedding_gateway=embedding_client,
        vector_index_writer=vector_index,
        embedding_model_version=settings.embedding.model,
        max_delivery_attempts=settings.rabbitmq.max_delivery_attempts,
    )
    resource_deletion_service = ResourceDeletionService(
        unit_of_work_factory=unit_of_work_factory,
        vector_index_writer=vector_index,
        max_delivery_attempts=settings.rabbitmq.max_delivery_attempts,
    )
    return IndexContainer(
        database=database,
        http_client=http_client,
        content_reader=content_reader,
        milvus_manager=milvus_manager,
        consumer=IndexEventConsumer(
            settings.rabbitmq,
            resource_service,
            resource_deletion_service,
            message_service,
        ),
    )
