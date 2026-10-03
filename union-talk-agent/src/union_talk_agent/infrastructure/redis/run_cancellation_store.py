"""Redis Run 取消快速信号。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:23
"""

import logging

from redis.asyncio import Redis
from redis.exceptions import RedisError

logger = logging.getLogger(__name__)


class RedisRunCancellationStore:
    """使用短 TTL Key 让生成 Worker 快速感知取消。"""

    def __init__(self, redis_client: Redis, ttl_seconds: int) -> None:
        """
        初始化 RedisRunCancellationStore

        :param redis_client: 异步 Redis 客户端
        :param ttl_seconds: 取消标记有效秒数
        :return: 无返回值
        """

        self._redis = redis_client
        self._ttl_seconds = ttl_seconds

    async def request_cancel(self, run_id: int) -> None:
        """
        写入取消信号；PostgreSQL 仍是最终状态真源

        :param run_id: Agent Run ID
        :return: 无返回值
        """

        try:
            await self._redis.set(
                self._key(run_id),
                "1",
                ex=self._ttl_seconds,
            )
        except RedisError:
            logger.warning("Redis取消信号写入失败, runId=%s", run_id, exc_info=True)

    async def is_cancel_requested(self, run_id: int) -> bool:
        """
        读取取消信号，Redis 故障时回退为未命中

        :param run_id: Agent Run ID
        :return: 是否已经请求取消
        """

        try:
            return bool(await self._redis.exists(self._key(run_id)))
        except RedisError:
            logger.warning("Redis取消信号读取失败, runId=%s", run_id, exc_info=True)
            return False

    @staticmethod
    def _key(run_id: int) -> str:
        """
        构建 Redis 取消标记键

        :param run_id: Agent Run ID
        :return: Redis 取消标记键
        """

        return f"ut:agent:run:{run_id}:cancel"


class UnavailableRunCancellationStore:
    """Redis 未启用时依赖阶段切换前的 PostgreSQL 校验。"""

    @staticmethod
    async def request_cancel(run_id: int) -> None:
        """
        无快速信号可写

        :param run_id: Agent Run ID
        :return: 无返回值
        """

        _ignored_run_id = run_id

    @staticmethod
    async def is_cancel_requested(run_id: int) -> bool:
        """
        返回未命中

        :param run_id: Agent Run ID
        :return: 是否已经请求取消
        """

        _ignored_run_id = run_id
        return False
