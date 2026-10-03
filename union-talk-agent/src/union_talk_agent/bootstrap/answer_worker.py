"""Answer Worker 进程入口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:08
"""

import asyncio
import logging

from union_talk_agent.bootstrap.container import build_container
from union_talk_agent.bootstrap.logging_config import configure_logging
from union_talk_agent.bootstrap.process_signals import install_stop_signal_handlers
from union_talk_agent.settings import load_settings

logger = logging.getLogger(__name__)


async def run() -> None:
    """
    初始化依赖并消费 AGENT_MENTIONED

    :return: 无返回值
    """

    settings = load_settings()
    configure_logging(settings.app.log_level)
    if not settings.app.answer_worker_enabled:
        logger.info("Answer Worker已通过配置停用")
        return
    container = build_container(settings)
    stop_event = asyncio.Event()
    install_stop_signal_handlers(stop_event)
    try:
        if container.milvus_manager is not None:
            await container.milvus_manager.ensure_collection()
        await container.answer_event_consumer.run(stop_event)
    finally:
        await container.close()


def main() -> None:
    """
    启动 Answer Worker

    :return: 无返回值
    """

    asyncio.run(run())
