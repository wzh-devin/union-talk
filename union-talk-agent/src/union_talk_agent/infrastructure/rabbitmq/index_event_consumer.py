"""文件与消息索引 RabbitMQ 消费者。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/13 00:01
"""

import asyncio
import logging

import aio_pika
from aio_pika import ExchangeType
from aio_pika.abc import AbstractIncomingMessage, AbstractQueue
from pydantic import ValidationError

from union_talk_agent.agent_run.domain.exceptions import NonRetryableAgentError
from union_talk_agent.interfaces.mq.constants import (
    ASSET_CONTENT_CHANGED_INDEX_QUEUE,
    ASSET_CONTENT_CHANGED_ROUTING_KEY,
    ASSET_DELETED_INDEX_QUEUE,
    ASSET_DELETED_ROUTING_KEY,
    FILE_EXCHANGE,
    MAX_EVENT_BYTES,
    MESSAGE_CREATED_INDEX_QUEUE,
    MESSAGE_CREATED_ROUTING_KEY,
    MESSAGE_DELETED_INDEX_QUEUE,
    MESSAGE_DELETED_ROUTING_KEY,
    MESSAGE_EXCHANGE,
    MESSAGE_RECALLED_INDEX_QUEUE,
    MESSAGE_RECALLED_ROUTING_KEY,
)
from union_talk_agent.interfaces.mq.schemas.asset_content_changed_event import (
    AssetContentChangedEvent,
)
from union_talk_agent.interfaces.mq.schemas.asset_deleted_event import AssetDeletedEvent
from union_talk_agent.interfaces.mq.schemas.message_index_event import MessageIndexEvent
from union_talk_agent.knowledge.application.message_indexing_service import (
    MessageIndexingService,
)
from union_talk_agent.knowledge.application.resource_deletion_service import (
    ResourceDeletionService,
)
from union_talk_agent.knowledge.application.resource_ingestion_service import (
    ResourceIngestionService,
)
from union_talk_agent.settings.rabbitmq_settings import RabbitMqSettings

logger = logging.getLogger(__name__)


class IndexEventConsumer:
    """隔离消费文件内容事件和消息索引事件。"""

    def __init__(
        self,
        settings: RabbitMqSettings,
        resource_service: ResourceIngestionService,
        resource_deletion_service: ResourceDeletionService,
        message_service: MessageIndexingService,
    ) -> None:
        """
        初始化索引事件消费者

        :param settings: RabbitMQ 配置
        :param resource_service: 资源入库服务
        :param resource_deletion_service: 资源删除服务
        :param message_service: 消息索引服务
        :return: 无返回值
        """

        self._settings = settings
        self._resource_service = resource_service
        self._resource_deletion_service = resource_deletion_service
        self._message_service = message_service
        self._connection: aio_pika.RobustConnection | None = None
        self._semaphore = asyncio.Semaphore(settings.consumer_concurrency)

    async def run(self, stop_event: asyncio.Event) -> None:
        """
        建立索引专用队列并消费到停止

        :param stop_event: 进程停止事件
        :return: 无返回值
        """

        self._connection = await aio_pika.connect_robust(self._settings.url)
        channel = await self._connection.channel()
        await channel.set_qos(prefetch_count=self._settings.prefetch_count)
        message_exchange = await channel.declare_exchange(
            MESSAGE_EXCHANGE,
            ExchangeType.TOPIC,
            durable=True,
        )
        file_exchange = await channel.declare_exchange(
            FILE_EXCHANGE,
            ExchangeType.TOPIC,
            durable=True,
        )
        resource_queue = await channel.declare_queue(
            ASSET_CONTENT_CHANGED_INDEX_QUEUE,
            durable=True,
        )
        await resource_queue.bind(
            file_exchange,
            routing_key=ASSET_CONTENT_CHANGED_ROUTING_KEY,
        )
        await resource_queue.consume(self._consume_resource)
        deleted_resource_queue = await channel.declare_queue(
            ASSET_DELETED_INDEX_QUEUE,
            durable=True,
        )
        await deleted_resource_queue.bind(
            file_exchange,
            routing_key=ASSET_DELETED_ROUTING_KEY,
        )
        await deleted_resource_queue.consume(self._consume_deleted_resource)
        await self._bind_message_queue(
            channel,
            message_exchange,
            MESSAGE_CREATED_INDEX_QUEUE,
            MESSAGE_CREATED_ROUTING_KEY,
        )
        await self._bind_message_queue(
            channel,
            message_exchange,
            MESSAGE_RECALLED_INDEX_QUEUE,
            MESSAGE_RECALLED_ROUTING_KEY,
        )
        await self._bind_message_queue(
            channel,
            message_exchange,
            MESSAGE_DELETED_INDEX_QUEUE,
            MESSAGE_DELETED_ROUTING_KEY,
        )
        logger.info("Index Worker开始消费文件和消息索引事件")
        await stop_event.wait()

    async def close(self) -> None:
        """
        关闭 RabbitMQ 连接

        :return: 无返回值
        """

        if self._connection is not None:
            await self._connection.close()
            self._connection = None

    async def _bind_message_queue(
        self,
        channel: aio_pika.abc.AbstractChannel,
        exchange: aio_pika.abc.AbstractExchange,
        queue_name: str,
        routing_key: str,
    ) -> AbstractQueue:
        """
        声明并绑定一个消息索引队列

        :param channel: RabbitMQ Channel
        :param exchange: 消息交换机
        :param queue_name: 队列名称
        :param routing_key: 路由键
        :return: 已绑定并开始消费的队列
        """

        queue = await channel.declare_queue(queue_name, durable=True)
        await queue.bind(exchange, routing_key=routing_key)
        await queue.consume(self._consume_message)
        return queue

    async def _consume_resource(self, message: AbstractIncomingMessage) -> None:
        """
        消费单条资源内容事件

        :param message: RabbitMQ 消息
        :return: 无返回值
        """

        async with self._semaphore:
            try:
                self._validate_size(message)
                event = AssetContentChangedEvent.model_validate_json(message.body)
                await self._resource_service.handle(event.to_command())
            except ValidationError:
                logger.warning("丢弃非法资源索引事件", exc_info=True)
                await message.reject(requeue=False)
                return
            except ValueError:
                logger.warning("丢弃超限资源索引事件", exc_info=True)
                await message.reject(requeue=False)
                return
            except NonRetryableAgentError as error:
                logger.warning(
                    "资源索引事件进入终态失败, errorCode=%s",
                    str(error.code),
                )
                await message.reject(requeue=False)
                return
            except Exception:
                logger.exception("资源索引事件处理失败")
                await asyncio.sleep(self._settings.retry_delay_seconds)
                await message.nack(requeue=True)
                return
            await message.ack()

    async def _consume_deleted_resource(self, message: AbstractIncomingMessage) -> None:
        """
        消费单条资产删除事件

        :param message: RabbitMQ 消息
        :return: 无返回值
        """

        async with self._semaphore:
            try:
                self._validate_size(message)
                event = AssetDeletedEvent.model_validate_json(message.body)
                await self._resource_deletion_service.handle(event.to_command())
            except ValidationError:
                logger.warning("丢弃非法资源删除事件", exc_info=True)
                await message.reject(requeue=False)
                return
            except ValueError:
                logger.warning("丢弃超限资源删除事件", exc_info=True)
                await message.reject(requeue=False)
                return
            except NonRetryableAgentError as error:
                logger.warning(
                    "资源删除事件进入终态失败, errorCode=%s",
                    str(error.code),
                )
                await message.reject(requeue=False)
                return
            except Exception:
                logger.exception("资源删除事件处理失败")
                await asyncio.sleep(self._settings.retry_delay_seconds)
                await message.nack(requeue=True)
                return
            await message.ack()

    async def _consume_message(self, message: AbstractIncomingMessage) -> None:
        """
        消费单条消息索引事件

        :param message: RabbitMQ 消息
        :return: 无返回值
        """

        async with self._semaphore:
            try:
                self._validate_size(message)
                event = MessageIndexEvent.model_validate_json(message.body)
                await self._message_service.handle(event.to_command())
            except ValidationError:
                logger.warning("丢弃非法消息索引事件", exc_info=True)
                await message.reject(requeue=False)
                return
            except ValueError:
                logger.warning("丢弃超限消息索引事件", exc_info=True)
                await message.reject(requeue=False)
                return
            except NonRetryableAgentError as error:
                logger.warning(
                    "消息索引事件进入终态失败, errorCode=%s",
                    str(error.code),
                )
                await message.reject(requeue=False)
                return
            except Exception:
                logger.exception("消息索引事件处理失败")
                await asyncio.sleep(self._settings.retry_delay_seconds)
                await message.nack(requeue=True)
                return
            await message.ack()

    @staticmethod
    def _validate_size(message: AbstractIncomingMessage) -> None:
        """
        拒绝超过契约上限的事件

        :param message: RabbitMQ 消息
        :return: 无返回值
        """

        if len(message.body) > MAX_EVENT_BYTES:
            raise ValueError("索引事件超过大小限制")
