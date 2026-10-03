"""Agent 进程显式依赖容器。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:07
"""

import os
import socket
import uuid
from dataclasses import dataclass

import httpx
from pymilvus import MilvusClient
from redis.asyncio import Redis

from union_talk_agent.agent_config.application.agent_config_service import (
    AgentControlPlaneService,
)
from union_talk_agent.agent_run.application.answer_run_service import AnswerRunService
from union_talk_agent.agent_run.application.prompt_assembler import PromptAssembler
from union_talk_agent.agent_run.application.reconciliation_service import (
    ReconciliationService,
)
from union_talk_agent.agent_run.application.run_cancellation_service import (
    RunCancellationService,
)
from union_talk_agent.agent_run.application.run_lifecycle_service import RunLifecycleService
from union_talk_agent.agent_run.application.run_query_service import RunQueryService
from union_talk_agent.agent_run.application.runtime_config_resolver import RuntimeConfigLoader
from union_talk_agent.agent_run.graph.answer_graph import AnswerGraph
from union_talk_agent.event_inbox.application.agent_mentioned_service import (
    AgentMentionedService,
)
from union_talk_agent.event_inbox.application.inbox_lifecycle_service import (
    InboxLifecycleService,
)
from union_talk_agent.infrastructure.crypto.api_key_cipher import ApiKeyCipher
from union_talk_agent.infrastructure.embedding.openai_embedding_client import (
    OpenAiEmbeddingClient,
)
from union_talk_agent.infrastructure.grpc.message_grpc_client import MessageGrpcClient
from union_talk_agent.infrastructure.id_generation.snowflake_id_generator import (
    SnowflakeIdGenerator,
)
from union_talk_agent.infrastructure.milvus.collection_manager import (
    MilvusCollectionManager,
)
from union_talk_agent.infrastructure.milvus.knowledge_retriever import (
    DisabledKnowledgeRetriever,
    MilvusKnowledgeRetriever,
)
from union_talk_agent.infrastructure.milvus.pymilvus_client_adapter import (
    PyMilvusClientAdapter,
)
from union_talk_agent.infrastructure.model.deepseek_chat_model import DeepSeekChatModel
from union_talk_agent.infrastructure.model.fixed_answer_chat_model import (
    FixedAnswerChatModel,
)
from union_talk_agent.infrastructure.nacos.nacos_service_registry import (
    NacosServiceRegistry,
)
from union_talk_agent.infrastructure.postgres.database import PostgresDatabase
from union_talk_agent.infrastructure.postgres.unit_of_work import (
    PostgresUnitOfWorkFactory,
)
from union_talk_agent.infrastructure.rabbitmq.answer_event_consumer import (
    AnswerEventConsumer,
)
from union_talk_agent.infrastructure.redis.redis_run_event_publisher import (
    RedisRunEventPublisher,
    UnavailableRunEventPublisher,
)
from union_talk_agent.infrastructure.redis.run_cancellation_store import (
    RedisRunCancellationStore,
    UnavailableRunCancellationStore,
)
from union_talk_agent.settings import Settings


@dataclass(slots=True)
class ApplicationContainer:
    """持有单进程共享连接池和应用服务。"""

    settings: Settings
    database: PostgresDatabase
    message_client: MessageGrpcClient
    deepseek_model: DeepSeekChatModel
    embedding_http_client: httpx.AsyncClient | None
    event_publisher: RedisRunEventPublisher | UnavailableRunEventPublisher
    milvus_manager: MilvusCollectionManager | None
    answer_graph: AnswerGraph
    answer_run_service: AnswerRunService
    reconciliation_service: ReconciliationService
    mentioned_service: AgentMentionedService
    control_plane_service: AgentControlPlaneService
    run_query_service: RunQueryService
    run_cancellation_service: RunCancellationService
    answer_event_consumer: AnswerEventConsumer
    service_registry: NacosServiceRegistry

    async def close(self) -> None:
        """
        按依赖逆序关闭所有连接池

        :return: 无返回值
        """

        await self.service_registry.close()
        await self.answer_event_consumer.close()
        if self.milvus_manager is not None:
            await self.milvus_manager.close()
        if self.embedding_http_client is not None:
            await self.embedding_http_client.aclose()
        await self.event_publisher.close()
        await self.deepseek_model.close()
        await self.message_client.close()
        await self.database.close()


def build_container(settings: Settings) -> ApplicationContainer:
    """
    构造不使用全局 Service Locator 的进程依赖图

    :param settings: Agent 应用配置
    :return: 完成依赖装配的应用容器
    """

    database = PostgresDatabase(settings.postgres)
    unit_of_work_factory = PostgresUnitOfWorkFactory(database.session_factory)
    id_generator = SnowflakeIdGenerator(
        settings.app.id_worker_id,
        settings.app.id_datacenter_id,
    )
    api_key_cipher = ApiKeyCipher(settings.app.api_key_master_key)

    message_client = MessageGrpcClient(
        settings.grpc.message_target,
        settings.grpc.deadline_seconds,
    )
    deepseek_model = DeepSeekChatModel(httpx.AsyncClient())
    selected_chat_model = (
        FixedAnswerChatModel(settings.app.fixed_answer_text)
        if settings.app.fixed_answer_enabled
        else deepseek_model
    )

    if settings.redis.enabled:
        redis_client = Redis.from_url(
            settings.redis.url,
            username=settings.redis.username or None,
            password=settings.redis.password.get_secret_value() or None,
            decode_responses=True,
        )
        event_publisher: RedisRunEventPublisher | UnavailableRunEventPublisher = (
            RedisRunEventPublisher(
                redis_client,
                settings.redis.run_ttl_seconds,
                settings.redis.stream_max_length,
            )
        )
        cancellation_store: RedisRunCancellationStore | UnavailableRunCancellationStore = (
            RedisRunCancellationStore(
                redis_client,
                settings.redis.run_ttl_seconds,
            )
        )
    else:
        event_publisher = UnavailableRunEventPublisher()
        cancellation_store = UnavailableRunCancellationStore()

    embedding_http_client: httpx.AsyncClient | None = None
    milvus_manager: MilvusCollectionManager | None = None
    if settings.milvus.enabled:
        milvus_client = PyMilvusClientAdapter(
            MilvusClient(
                uri=settings.milvus.uri,
                token=settings.milvus.token,
            )
        )
        embedding_http_client = httpx.AsyncClient()
        embedding_client = OpenAiEmbeddingClient(
            embedding_http_client,
            settings.embedding.api_base,
            settings.embedding.api_key,
            settings.embedding.model,
            settings.embedding.dimension,
            settings.embedding.timeout_seconds,
        )
        milvus_manager = MilvusCollectionManager(
            milvus_client,
            settings.milvus.collection_name,
            settings.embedding.dimension,
        )
        knowledge_retriever = MilvusKnowledgeRetriever(
            client=milvus_client,
            collection_name=settings.milvus.collection_name,
            embedding_client=embedding_client,
            unit_of_work_factory=unit_of_work_factory,
        )
    else:
        knowledge_retriever = DisabledKnowledgeRetriever()

    lifecycle_service = RunLifecycleService(
        unit_of_work_factory,
        id_generator,
        event_publisher,
        cancellation_store,
    )
    runtime_config_loader = RuntimeConfigLoader(
        unit_of_work_factory,
        api_key_cipher,
        settings.app.fixed_answer_enabled,
        settings.model.deepseek_api_base,
        settings.model.deepseek_model_id,
        settings.model.timeout_seconds,
        settings.model.max_retries,
    )
    answer_graph = AnswerGraph(
        conversation_reader=message_client,
        knowledge_retriever=knowledge_retriever,
        chat_model=selected_chat_model,
        message_reply_writer=message_client,
        prompt_assembler=PromptAssembler(),
        lifecycle_service=lifecycle_service,
        fixed_answer_enabled=settings.app.fixed_answer_enabled,
    )
    worker_id = f"{socket.gethostname()}:{os.getpid()}:{uuid.uuid4().hex[:8]}"
    answer_run_service = AnswerRunService(
        unit_of_work_factory=unit_of_work_factory,
        runtime_config_loader=runtime_config_loader,
        answer_graph=answer_graph,
        lifecycle_service=lifecycle_service,
        worker_id=worker_id,
        lease_seconds=settings.app.run_lease_seconds,
        heartbeat_seconds=settings.app.run_heartbeat_seconds,
    )
    inbox_lifecycle_service = InboxLifecycleService(unit_of_work_factory)
    mentioned_service = AgentMentionedService(
        unit_of_work_factory=unit_of_work_factory,
        id_generator=id_generator,
        answer_run_service=answer_run_service,
        inbox_lifecycle_service=inbox_lifecycle_service,
        fixed_answer_enabled=settings.app.fixed_answer_enabled,
        max_delivery_attempts=settings.rabbitmq.max_delivery_attempts,
    )
    control_plane_service = AgentControlPlaneService(
        unit_of_work_factory=unit_of_work_factory,
        id_generator=id_generator,
        api_key_cipher=api_key_cipher,
        deepseek_chat_model=deepseek_model,
    )
    run_query_service = RunQueryService(unit_of_work_factory, event_publisher)
    run_cancellation_service = RunCancellationService(
        unit_of_work_factory,
        lifecycle_service,
    )
    answer_event_consumer = AnswerEventConsumer(settings.rabbitmq, mentioned_service)
    reconciliation_service = ReconciliationService(
        unit_of_work_factory,
        answer_run_service,
    )
    service_registry = NacosServiceRegistry(settings.nacos, settings.app.api_port)
    return ApplicationContainer(
        settings=settings,
        database=database,
        message_client=message_client,
        deepseek_model=deepseek_model,
        embedding_http_client=embedding_http_client,
        event_publisher=event_publisher,
        milvus_manager=milvus_manager,
        answer_graph=answer_graph,
        answer_run_service=answer_run_service,
        reconciliation_service=reconciliation_service,
        mentioned_service=mentioned_service,
        control_plane_service=control_plane_service,
        run_query_service=run_query_service,
        run_cancellation_service=run_cancellation_service,
        answer_event_consumer=answer_event_consumer,
        service_registry=service_registry,
    )
