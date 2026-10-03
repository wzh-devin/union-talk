"""AGENT_MENTIONED RabbitMQ 消费者。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:07
"""

import asyncio
import logging

import aio_pika
from aio_pika import ExchangeType
from aio_pika.abc import AbstractIncomingMessage
from pydantic import ValidationError
from sqlalchemy.exc import SQLAlchemyError

from union_talk_agent.agent_run.domain.exceptions import (
    AgentDomainError,
    NonRetryableAgentError,
    RetryableAgentError,
)
from union_talk_agent.event_inbox.application.agent_mentioned_service import (
    AgentMentionedService,
)
from union_talk_agent.interfaces.mq.constants import (
    AGENT_MENTIONED_QUEUE,
    AGENT_MENTIONED_ROUTING_KEY,
    MAX_EVENT_BYTES,
    MESSAGE_EXCHANGE,
)
from union_talk_agent.interfaces.mq.schemas.agent_mentioned_event import (
    AgentMentionedEvent,
)
from union_talk_agent.settings.rabbitmq_settings import RabbitMqSettings

logger = logging.getLogger(__name__)


class AnswerEventConsumer:
    """消费提及事件并根据错误分类执行 ACK 或重投。"""

    def __init__(
        self,
        settings: RabbitMqSettings,
        mentioned_service: AgentMentionedService,
    ) -> None:
        """
        初始化 AnswerEventConsumer

        :param settings: Agent 应用配置
        :param mentioned_service: Agent 提及事件处理服务
        :return: 无返回值
        """

        self._settings = settings
        self._mentioned_service = mentioned_service
        self._connection: aio_pika.RobustConnection | None = None
        self._semaphore = asyncio.Semaphore(settings.consumer_concurrency)

    async def run(self, stop_event: asyncio.Event) -> None:
        """
        建立 Robust 连接并持续消费到收到停止信号

        :param stop_event: 进程协作式停止事件
        :return: 无返回值
        """

        self._connection = await aio_pika.connect_robust(self._settings.url)
        channel = await self._connection.channel()
        await channel.set_qos(prefetch_count=self._settings.prefetch_count)
        exchange = await channel.declare_exchange(
            MESSAGE_EXCHANGE,
            ExchangeType.TOPIC,
            durable=True,
        )
        queue = await channel.declare_queue(
            AGENT_MENTIONED_QUEUE,
            durable=True,
        )
        await queue.bind(exchange, routing_key=AGENT_MENTIONED_ROUTING_KEY)
        await queue.consume(self._consume)
        logger.info("Agent Answer Worker开始消费提及事件")
        await stop_event.wait()

    async def close(self) -> None:
        """
        关闭 RabbitMQ 连接

        :return: 无返回值
        """

        if self._connection is not None:
            await self._connection.close()
            self._connection = None

    async def _consume(self, message: AbstractIncomingMessage) -> None:
        """
        消费并处理单条 Agent 事件

        :param message: 待发布的消息内容
        :return: 无返回值
        """

        async with self._semaphore:
            if len(message.body) > MAX_EVENT_BYTES:
                logger.warning("丢弃超出大小限制的Agent事件")
                await message.reject(requeue=False)
                return
            try:
                event = AgentMentionedEvent.model_validate_json(message.body)
            except ValidationError:
                logger.warning("丢弃契约非法的Agent事件", exc_info=True)
                await message.reject(requeue=False)
                return
            try:
                run_id = await self._mentioned_service.handle(event.to_command())
            except RetryableAgentError as error:
                logger.warning(
                    "Agent事件处理失败并重投, eventId=%s, errorCode=%s",
                    event.event_id,
                    str(error.code),
                )
                await message.nack(requeue=True)
                return
            except NonRetryableAgentError as error:
                logger.warning(
                    "Agent事件被终态拒绝, eventId=%s, errorCode=%s",
                    event.event_id,
                    str(error.code),
                )
                await message.reject(requeue=False)
                return
            except (AgentDomainError, SQLAlchemyError, OSError):
                logger.exception("Agent事件出现未分类异常, eventId=%s", event.event_id)
                await message.nack(requeue=True)
                return
            await message.ack()
            logger.info(
                "Agent事件处理完成, eventId=%s, runId=%s",
                event.event_id,
                run_id,
            )
