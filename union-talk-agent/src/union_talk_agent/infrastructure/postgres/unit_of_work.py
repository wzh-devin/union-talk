"""SQLAlchemy Agent Unit of Work。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:52
"""

from types import TracebackType

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentControlPlaneStore,
    AgentRunStore,
    AgentTraceStore,
    AgentUnitOfWork,
    AssetLifecycleStore,
    BusinessLockStore,
    InboxEventStore,
    KnowledgeEvidenceStore,
    KnowledgeIndexStore,
    MessageSegmentStore,
)
from union_talk_agent.infrastructure.postgres.dao.agent_config_dao import AgentControlPlaneDao
from union_talk_agent.infrastructure.postgres.dao.agent_run_dao import AgentRunDao
from union_talk_agent.infrastructure.postgres.dao.agent_trace_dao import AgentTraceDao
from union_talk_agent.infrastructure.postgres.dao.asset_lifecycle_dao import AssetLifecycleDao
from union_talk_agent.infrastructure.postgres.dao.business_lock_dao import BusinessLockDao
from union_talk_agent.infrastructure.postgres.dao.event_inbox_dao import EventInboxDao
from union_talk_agent.infrastructure.postgres.dao.knowledge_evidence_dao import (
    KnowledgeEvidenceDao,
)
from union_talk_agent.infrastructure.postgres.dao.message_segment_dao import MessageSegmentDao
from union_talk_agent.infrastructure.postgres.dao.resource_index_dao import ResourceIndexDao


class PostgresUnitOfWork:
    """请求级 PostgreSQL 事务和 DAO 集合。"""

    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        """
        初始化 PostgresUnitOfWork

        :param session_factory: 异步数据库会话工厂
        :return: 无返回值
        """

        self._session_factory = session_factory
        self._session: AsyncSession | None = None
        self._is_committed = False
        self.business_locks: BusinessLockStore
        self.inbox_events: InboxEventStore
        self.agent_runs: AgentRunStore
        self.agent_control_plane: AgentControlPlaneStore
        self.agent_traces: AgentTraceStore
        self.knowledge_evidence: KnowledgeEvidenceStore
        self.knowledge_index: KnowledgeIndexStore
        self.asset_lifecycles: AssetLifecycleStore
        self.message_segments: MessageSegmentStore

    async def __aenter__(self) -> "PostgresUnitOfWork":
        """
        开启事务并创建请求级 DAO

        :return: 当前 Unit of Work
        """

        self._session = self._session_factory()
        await self._session.begin()
        self.business_locks = BusinessLockDao(self._session)
        self.inbox_events = EventInboxDao(self._session)
        self.agent_runs = AgentRunDao(self._session)
        self.agent_control_plane = AgentControlPlaneDao(self._session)
        self.agent_traces = AgentTraceDao(self._session)
        self.knowledge_evidence = KnowledgeEvidenceDao(self._session)
        self.knowledge_index = ResourceIndexDao(self._session)
        self.asset_lifecycles = AssetLifecycleDao(self._session)
        self.message_segments = MessageSegmentDao(self._session)
        return self

    async def __aexit__(
        self,
        exc_type: type[BaseException] | None,
        exc_value: BaseException | None,
        traceback: TracebackType | None,
    ) -> None:
        """
        异常或未显式提交时回滚并关闭 Session

        :param exc_type: 上下文退出时的异常类型
        :param exc_value: 上下文退出时的异常实例
        :param traceback: 上下文退出时的异常堆栈
        :return: 无返回值
        """

        if self._session is None:
            return
        if exc_type is not None or not self._is_committed:
            await self._session.rollback()
        await self._session.close()
        self._session = None
        self._is_committed = False

    async def commit(self) -> None:
        """
        提交事务

        :return: 无返回值
        """

        if self._session is None:
            raise RuntimeError("Unit of Work 尚未进入事务")
        await self._session.commit()
        self._is_committed = True


class PostgresUnitOfWorkFactory:
    """创建独立 PostgreSQL Unit of Work。"""

    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        """
        初始化 PostgresUnitOfWorkFactory

        :param session_factory: 异步数据库会话工厂
        :return: 无返回值
        """

        self._session_factory = session_factory

    def __call__(self) -> AgentUnitOfWork:
        """
        创建 Unit of Work

        :return: 尚未开启事务的 Unit of Work
        """

        return PostgresUnitOfWork(self._session_factory)
