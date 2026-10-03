"""Redis 实时链路就绪状态测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/31 17:10
"""

from typing import cast

from redis.asyncio import Redis
from redis.exceptions import AuthenticationError

from union_talk_agent.infrastructure.redis.redis_run_event_publisher import (
    RedisRunEventPublisher,
)


class RedisPingStub:
    """提供可控结果的 Redis PING 测试替身。"""

    def __init__(self, error: AuthenticationError | None = None) -> None:
        """
        初始化 Redis PING 测试替身

        :param error: PING 时需要抛出的鉴权异常
        :return: 无返回值
        """

        self._error = error

    async def ping(self) -> bool:
        """
        返回成功结果或抛出预设异常

        :return: Redis PING 是否成功
        """

        if self._error is not None:
            raise self._error
        return True


async def test_redis_publisher_reports_ready_after_successful_ping() -> None:
    """
    Redis PING 成功时实时链路应处于就绪状态

    :return: 无返回值
    """

    redis_client = cast(Redis, cast(object, RedisPingStub()))
    publisher = RedisRunEventPublisher(redis_client, 86400, 2000)

    assert await publisher.check() is True


async def test_redis_publisher_reports_not_ready_when_authentication_fails() -> None:
    """
    Redis 鉴权失败时实时链路不得被健康检查判定为就绪

    :return: 无返回值
    """

    redis_client = cast(
        Redis,
        cast(
            object,
            RedisPingStub(AuthenticationError("authentication required")),
        ),
    )
    publisher = RedisRunEventPublisher(redis_client, 86400, 2000)

    assert await publisher.check() is False
