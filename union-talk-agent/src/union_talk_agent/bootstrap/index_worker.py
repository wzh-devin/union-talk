"""Index Worker 进程入口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:08
"""

import asyncio
import logging

from union_talk_agent.bootstrap.index_container import build_index_container
from union_talk_agent.bootstrap.logging_config import configure_logging
from union_talk_agent.bootstrap.process_signals import install_stop_signal_handlers
from union_talk_agent.settings import load_settings

logger = logging.getLogger(__name__)


async def run() -> None:
    """
    启动资源和消息索引消费者

    :return: 无返回值
    """

    settings = load_settings()
    configure_logging(settings.app.log_level)
    if not settings.app.index_worker_enabled:
        logger.info("Index Worker已通过配置停用")
        return
    container = build_index_container(settings)
    stop_event = asyncio.Event()
    install_stop_signal_handlers(stop_event)
    try:
        await container.milvus_manager.ensure_collection()
        await container.consumer.run(stop_event)
    finally:
        await container.close()


def main() -> None:
    """
    运行 Index Worker 事件循环

    :return: 无返回值
    """

    asyncio.run(run())
