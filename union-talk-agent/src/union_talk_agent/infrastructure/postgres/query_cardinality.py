"""PostgreSQL 查询基数校验。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 19:20
"""

from typing import Any

import sqlalchemy as sa
from sqlalchemy.engine import RowMapping
from sqlalchemy.ext.asyncio import AsyncSession

from union_talk_agent.agent_run.domain.exceptions import AgentDomainError, AgentErrorCode


async def select_one_or_none(
    session: AsyncSession,
    statement: sa.Select[Any],
    duplicate_message: str,
) -> RowMapping | None:
    """
    查询至多一条记录并拒绝数据库中的重复业务键

    :param session: 当前异步数据库会话
    :param statement: 不包含数量限制的查询语句
    :param duplicate_message: 发现重复记录时的错误摘要
    :return: 唯一数据库行，不存在时返回空
    """

    result = await session.execute(statement.limit(2))
    row_list = result.mappings().all()
    if len(row_list) > 1:
        raise AgentDomainError(
            AgentErrorCode.INFRASTRUCTURE_UNAVAILABLE,
            duplicate_message,
        )
    return row_list[0] if row_list else None
