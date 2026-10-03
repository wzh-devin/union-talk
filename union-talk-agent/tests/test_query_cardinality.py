"""PostgreSQL 查询基数校验测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 19:28
"""

from typing import cast
from unittest.mock import AsyncMock, MagicMock

import pytest
import sqlalchemy as sa
from sqlalchemy.ext.asyncio import AsyncSession

from union_talk_agent.agent_run.domain.exceptions import AgentDomainError
from union_talk_agent.infrastructure.postgres.query_cardinality import select_one_or_none


def build_session_with_rows(row_list: list[dict[str, object]]) -> AsyncSession:
    """
    构造返回指定数据库行的异步会话替身

    :param row_list: 查询返回的数据库行列表
    :return: 可用于基数校验的异步会话替身
    """

    session = AsyncMock(spec=AsyncSession)
    result = MagicMock()
    result.mappings.return_value.all.return_value = row_list
    session.execute.return_value = result
    return cast(AsyncSession, session)


async def test_select_one_or_none_returns_single_row() -> None:
    """
    验证唯一业务记录可以正常返回

    :return: 无返回值
    """

    row = await select_one_or_none(
        build_session_with_rows([{"id": 1}]),
        sa.select(sa.literal(1)),
        "发现重复记录",
    )

    assert row is not None
    assert row["id"] == 1


async def test_select_one_or_none_rejects_duplicate_business_rows() -> None:
    """
    验证无唯一约束时重复业务记录不会被 LIMIT 1 掩盖

    :return: 无返回值
    """

    with pytest.raises(AgentDomainError, match="发现重复记录"):
        await select_one_or_none(
            build_session_with_rows([{"id": 1}, {"id": 2}]),
            sa.select(sa.literal(1)),
            "发现重复记录",
        )
