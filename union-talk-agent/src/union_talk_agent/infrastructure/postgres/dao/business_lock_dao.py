"""PostgreSQL 事务级业务锁 DAO。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:52
"""

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession


class BusinessLockDao:
    """使用 `pg_advisory_xact_lock` 串行业务键。"""

    def __init__(self, session: AsyncSession) -> None:
        """
        初始化 BusinessLockDao

        :param session: 异步数据库会话
        :return: 无返回值
        """

        self._session = session

    async def lock(self, namespace: str, business_key: str) -> None:
        """
        获取事务级业务锁

        :param namespace: 稳定业务命名空间
        :param business_key: 业务键
        :return: 无返回值
        """

        await self._session.execute(
            text(
                """
                SELECT pg_advisory_xact_lock(
                    hashtextextended(:lock_key, 0)
                )
                """
            ),
            {"lock_key": f"{namespace}:{business_key}"},
        )
