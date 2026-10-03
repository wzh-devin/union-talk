"""基于 Redis Snapshot、Stream 与 Pub/Sub 的实时事件实现。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:56
"""

import logging
from typing import Any, cast

import orjson
from redis.asyncio import Redis
from redis.exceptions import RedisError, WatchError

from union_talk_agent.agent_run.application.ports.run_event_publisher import (
    RunEventReadError,
)
from union_talk_agent.agent_run.realtime.run_event import (
    AgentRunEvent,
    AgentRunSnapshot,
    StoredRunEvent,
)

logger = logging.getLogger(__name__)

type RedisStreamFields = dict[str | bytes, str | bytes]
type RedisStreamEntry = tuple[str | bytes, RedisStreamFields]
type RedisStreamResult = list[tuple[str | bytes, list[RedisStreamEntry]]]


class _RedisTransactionQueue:
    """Redis MULTI 后只入队、不立即执行的同步命令视图。"""

    def __init__(self, pipeline: object) -> None:
        """
        初始化 _RedisTransactionQueue

        :param pipeline: 已进入 MULTI 状态的 Redis Pipeline
        :return: 无返回值
        """

        self._pipeline: Any = pipeline

    def set(self, name: str, value: object) -> object:
        """
        入队 SET

        :param name: Redis 键或资源名称
        :param value: 待转换或序列化的值
        :return: 入队后的 Redis Pipeline
        """

        return self._pipeline.set(name, value)

    def expire(self, name: str, time: int) -> object:
        """
        入队 EXPIRE

        :param name: Redis 键或资源名称
        :param time: Redis 键有效秒数
        :return: 入队后的 Redis Pipeline
        """

        return self._pipeline.expire(name, time)

    def xadd(
        self,
        name: str,
        fields: dict[str, str],
        *,
        maxlen: int,
        approximate: bool,
    ) -> object:
        """
        入队 XADD

        :param name: Redis 键或资源名称
        :param fields: Redis Stream 字段映射
        :param maxlen: Redis Stream 最大长度
        :param approximate: 是否使用近似裁剪
        :return: 入队后的 Redis Pipeline
        """

        return self._pipeline.xadd(
            name,
            fields,
            maxlen=maxlen,
            approximate=approximate,
        )

    def publish(self, channel: str, message: str) -> object:
        """
        入队 PUBLISH

        :param channel: Redis 广播频道
        :param message: 待发布的消息内容
        :return: 入队后的 Redis Pipeline
        """

        return self._pipeline.publish(channel, message)


class RedisRunEventPublisher:
    """保存卡片快照并发布可重放的 Run 事件。"""

    _CHANNEL = "ut:agent:live"

    def __init__(
        self,
        redis_client: Redis,
        run_ttl_seconds: int,
        stream_max_length: int,
    ) -> None:
        """
        初始化 RedisRunEventPublisher

        :param redis_client: 异步 Redis 客户端
        :param run_ttl_seconds: Agent 实时数据有效秒数
        :param stream_max_length: Redis Stream 最大长度
        :return: 无返回值
        """

        self._redis = redis_client
        self._run_ttl_seconds = run_ttl_seconds
        self._stream_max_length = stream_max_length

    async def check(self) -> bool:
        """
        使用 PING 检查 Redis 实时链路

        :return: Redis 是否可以完成已认证命令
        """

        try:
            return bool(await self._redis.ping())
        except RedisError:
            return False

    async def publish(self, event: AgentRunEvent) -> AgentRunEvent:
        """
        原子分配序号并更新 Snapshot、Stream 与 Pub/Sub

        Redis 故障只会降低实时体验，调用方仍可继续正式消息落库。

        :param event: 待处理的 Agent 实时事件
        :return: 已分配稳定序号的事件；Redis 不可用时返回原事件
        """

        try:
            return await self._publish_atomically(event)
        except (RedisError, TypeError, ValueError):
            logger.warning(
                "Redis实时事件发布失败, runId=%s, eventType=%s",
                event.run_id,
                event.event_type,
                exc_info=True,
            )
            return event

    async def _publish_atomically(self, event: AgentRunEvent) -> AgentRunEvent:
        """
        原子更新 Redis 快照、事件流和广播

        :param event: 待处理的 Agent 实时事件
        :return: 已分配稳定序号并完成原子发布的事件
        """

        sequence_key = self._sequence_key(event.run_id)
        snapshot_key = self._snapshot_key(event.run_id)
        published_event: AgentRunEvent | None = None
        while published_event is None:
            async with self._redis.pipeline(transaction=True) as pipeline:
                try:
                    await pipeline.watch(sequence_key, snapshot_key)
                    raw_sequence = await pipeline.get(sequence_key)
                    raw_snapshot = await pipeline.get(snapshot_key)
                    snapshot = (
                        AgentRunSnapshot.model_validate_json(raw_snapshot)
                        if raw_snapshot is not None
                        else None
                    )
                    if snapshot is not None and not snapshot.accepts(event):
                        await pipeline.unwatch()
                        published_event = event.model_copy(
                            update={
                                "sequence": snapshot.last_sequence,
                                "event_id": f"{event.run_id}:{snapshot.last_sequence}",
                            }
                        )
                        continue
                    sequence = int(raw_sequence or 0) + 1
                    event_with_sequence = event.model_copy(
                        update={
                            "sequence": sequence,
                            "event_id": f"{event.run_id}:{sequence}",
                        }
                    )
                    next_snapshot = (
                        snapshot.apply(event_with_sequence)
                        if snapshot is not None
                        else AgentRunSnapshot.from_event(event_with_sequence)
                    )
                    event_json = self._encode(event_with_sequence)
                    pipeline.multi()
                    transaction_queue = _RedisTransactionQueue(pipeline)
                    transaction_queue.set(sequence_key, sequence)
                    transaction_queue.expire(
                        sequence_key,
                        self._run_ttl_seconds,
                    )
                    transaction_queue.set(
                        snapshot_key,
                        self._encode(next_snapshot),
                    )
                    transaction_queue.expire(
                        snapshot_key,
                        self._run_ttl_seconds,
                    )
                    transaction_queue.xadd(
                        self._stream_key(event.run_id),
                        {"event": event_json},
                        maxlen=self._stream_max_length,
                        approximate=True,
                    )
                    transaction_queue.expire(
                        self._stream_key(event.run_id),
                        self._run_ttl_seconds,
                    )
                    transaction_queue.publish(self._CHANNEL, event_json)
                    await pipeline.execute()
                    published_event = event_with_sequence
                except WatchError:
                    continue
        return published_event

    async def get_snapshot(self, run_id: int) -> dict[str, object] | None:
        """
        读取可直接返回给前端的卡片快照

        :param run_id: Agent Run ID
        :return: Agent 卡片快照；不可用时返回空
        """

        try:
            snapshot = await self._load_snapshot_model(run_id)
            return snapshot.model_dump(by_alias=True, mode="json") if snapshot else None
        except (RedisError, TypeError, ValueError):
            logger.warning("Redis卡片快照读取失败, runId=%s", run_id, exc_info=True)
            return None

    async def read_after(
        self,
        run_id: int,
        stream_id: str,
        block_milliseconds: int,
    ) -> list[StoredRunEvent]:
        """
        使用 Redis Stream 完成断线重放和阻塞读取

        :param run_id: Agent Run ID
        :param stream_id: Redis Stream 游标
        :param block_milliseconds: Redis Stream 阻塞读取毫秒数
        :return: 指定游标后的实时事件列表
        """

        try:
            result = cast(
                RedisStreamResult,
                await self._redis.xread(
                    {self._stream_key(run_id): stream_id},
                    count=100,
                    block=block_milliseconds,
                ),
            )
        except RedisError as error:
            raise RunEventReadError("Redis Run事件读取失败") from error
        stored_event_list: list[StoredRunEvent] = []
        for _, entry_list in result:
            for raw_stream_id, field_map in entry_list:
                event_payload = field_map.get("event") or field_map.get(b"event")
                if event_payload is None:
                    continue
                stored_event_list.append(
                    StoredRunEvent(
                        stream_id=self._as_text(raw_stream_id),
                        event=AgentRunEvent.model_validate_json(event_payload),
                    )
                )
        return stored_event_list

    async def close(self) -> None:
        """
        关闭 Redis 连接池

        :return: 无返回值
        """

        await self._redis.aclose()

    async def _load_snapshot_model(self, run_id: int) -> AgentRunSnapshot | None:
        """
        读取并解析 Redis 卡片快照

        :param run_id: Agent Run ID
        :return: Agent 卡片快照，不存在时返回空
        """

        raw_snapshot = await self._redis.get(self._snapshot_key(run_id))
        if raw_snapshot is None:
            return None
        return AgentRunSnapshot.model_validate_json(raw_snapshot)

    @staticmethod
    def _encode(model: AgentRunEvent | AgentRunSnapshot) -> str:
        """
        将实时模型编码为 JSON 文本

        :param model: 待序列化的实时模型
        :return: JSON 文本
        """

        return orjson.dumps(model.model_dump(by_alias=True, mode="json")).decode()

    @staticmethod
    def _as_text(value: str | bytes) -> str:
        """
        将 Redis 字段值转换为文本

        :param value: 待转换或序列化的值
        :return: 解码后的文本
        """

        return value.decode() if isinstance(value, bytes) else value

    @staticmethod
    def _snapshot_key(run_id: int) -> str:
        """
        构建 Redis 卡片快照键

        :param run_id: Agent Run ID
        :return: Redis 卡片快照键
        """

        return f"ut:agent:run:{run_id}:live"

    @staticmethod
    def _stream_key(run_id: int) -> str:
        """
        构建 Redis 事件流键

        :param run_id: Agent Run ID
        :return: Redis 事件流键
        """

        return f"ut:agent:run:{run_id}:events"

    @staticmethod
    def _sequence_key(run_id: int) -> str:
        """
        构建 Redis 事件序号键

        :param run_id: Agent Run ID
        :return: Redis 事件序号键
        """

        return f"ut:agent:run:{run_id}:sequence"


class UnavailableRunEventPublisher:
    """Redis 未启用时维持业务闭环的降级实现。"""

    @staticmethod
    async def check() -> bool:
        """
        返回未启用实时事件存储的状态

        :return: 固定返回否
        """

        return False

    @staticmethod
    async def publish(event: AgentRunEvent) -> AgentRunEvent:
        """
        忽略实时事件并返回原事件

        :param event: 待处理的 Agent 实时事件
        :return: 未发布的原始事件
        """

        return event

    @staticmethod
    async def get_snapshot(run_id: int) -> dict[str, object] | None:
        """
        Redis 未启用时不存在实时快照

        :param run_id: Agent Run ID
        :return: Agent 卡片快照；不可用时返回空
        """

        _ignored_run_id = run_id
        return None

    @staticmethod
    async def read_after(
        run_id: int,
        stream_id: str,
        block_milliseconds: int,
    ) -> list[StoredRunEvent]:
        """
        Redis 未启用时拒绝建立 SSE 重放

        :param run_id: Agent Run ID
        :param stream_id: Redis Stream 游标
        :param block_milliseconds: Redis Stream 阻塞读取毫秒数
        :return: 指定游标后的实时事件列表
        """

        _ignored_arguments = run_id, stream_id, block_milliseconds
        raise RunEventReadError("Redis 实时事件未启用")

    @staticmethod
    async def close() -> None:
        """
        无连接需要关闭

        :return: 无返回值
        """
