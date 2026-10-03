"""SQLAlchemy AsyncEngine 生命周期。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:49
"""

from sqlalchemy import text
from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)

from union_talk_agent.settings.postgres_settings import PostgresSettings


class PostgresDatabase:
    """Agent PostgreSQL 连接池。"""

    def __init__(self, settings: PostgresSettings) -> None:
        """
        初始化 PostgresDatabase

        :param settings: Agent 应用配置
        :return: 无返回值
        """

        self._engine: AsyncEngine = create_async_engine(
            settings.dsn,
            pool_size=settings.pool_size,
            max_overflow=settings.max_overflow,
            pool_timeout=settings.pool_timeout_seconds,
            pool_pre_ping=True,
        )
        self._session_factory = async_sessionmaker(
            bind=self._engine,
            class_=AsyncSession,
            expire_on_commit=False,
        )

    @property
    def session_factory(self) -> async_sessionmaker[AsyncSession]:
        """
        获取异步 Session 工厂

        :return: SQLAlchemy 异步 Session 工厂
        """

        return self._session_factory

    async def check(self) -> bool:
        """
        检查数据库连接

        :return: 数据库是否可用
        """

        async with self._session_factory() as session:
            result = await session.execute(text("SELECT 1"))
            return result.scalar_one() == 1

    async def close(self) -> None:
        """
        关闭数据库连接池

        :return: 无返回值
        """

        await self._engine.dispose()
