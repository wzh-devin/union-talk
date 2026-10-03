"""Agent Alembic 运行环境。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:54
"""

from __future__ import annotations

import asyncio
from logging.config import fileConfig
from typing import Any

import sqlalchemy as sa
from alembic.ddl.postgresql import PostgresqlImpl
from sqlalchemy import pool
from sqlalchemy.engine import Connection
from sqlalchemy.ext.asyncio import async_engine_from_config

from alembic import context
from union_talk_agent.infrastructure.postgres.tables import metadata
from union_talk_agent.settings import load_settings

config = context.config
if config.config_file_name is not None:
    fileConfig(config.config_file_name)

config.set_main_option("sqlalchemy.url", load_settings().postgres.dsn)
target_metadata = metadata


class AgentPostgresqlImpl(PostgresqlImpl):
    """覆盖 Alembic 默认的 NOT NULL 版本表。"""

    __dialect__ = "postgresql"

    def version_table_impl(
        self,
        *,
        version_table: str,
        version_table_schema: str | None,
        version_table_pk: bool,
        **kwargs: Any,
    ) -> sa.Table:
        """
        生成仅含普通索引和字段注释的版本表

        :param version_table: Alembic 版本表
        :param version_table_schema: Alembic 版本表 Schema
        :param version_table_pk: 是否创建 Alembic 版本表主键
        :param kwargs: 透传的关键字选项
        :return: 不含主键约束的 Alembic 版本表
        """

        version_metadata = sa.MetaData()
        table = sa.Table(
            version_table,
            version_metadata,
            sa.Column(
                "version_num",
                sa.String(32),
                nullable=True,
                comment="当前迁移版本号",
            ),
            schema=version_table_schema,
            comment="Agent数据库迁移版本",
        )
        sa.Index("idx_agent_alembic_version_num", table.c.version_num)
        return table


def configure_context(**kwargs: object) -> None:
    """
    配置无约束的独立 Agent 版本表

    :param kwargs: 透传的关键字选项
    :return: 无返回值
    """

    context.configure(
        target_metadata=target_metadata,
        include_schemas=True,
        version_table="ut_agent_alembic_version",
        version_table_schema="public",
        version_table_pk=False,
        **kwargs,
    )


def run_migrations_offline() -> None:
    """
    在不建立数据库连接时生成迁移 SQL

    :return: 无返回值
    """

    configure_context(
        url=config.get_main_option("sqlalchemy.url"),
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
    )
    with context.begin_transaction():
        context.run_migrations()


def run_sync_migrations(connection: Connection) -> None:
    """
    在 Alembic 同步连接包装中执行迁移

    :param connection: 数据库连接
    :return: 无返回值
    """

    configure_context(
        connection=connection,
        compare_type=True,
        compare_server_default=True,
    )
    with context.begin_transaction():
        context.run_migrations()


async def run_migrations_online() -> None:
    """
    建立异步数据库连接并执行迁移

    :return: 无返回值
    """

    connectable = async_engine_from_config(
        config.get_section(config.config_ini_section, {}),
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )
    async with connectable.connect() as connection:
        await connection.run_sync(run_sync_migrations)
    await connectable.dispose()


if context.is_offline_mode():
    run_migrations_offline()
else:
    asyncio.run(run_migrations_online())
