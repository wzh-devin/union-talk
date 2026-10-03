"""Reconciliation Worker 进程入口。

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
    周期扫描并恢复 QUEUED 或租约过期的 Run

    :return: 无返回值
    """

    settings = load_settings()
    configure_logging(settings.app.log_level)
    if not settings.app.reconciliation_worker_enabled:
        logger.info("Reconciliation Worker已通过配置停用")
        return
    container = build_container(settings)
    stop_event = asyncio.Event()
    install_stop_signal_handlers(stop_event)
    try:
        while not stop_event.is_set():
            executed_count = await container.reconciliation_service.execute_batch()
            if executed_count:
                logger.info("Agent Run对账完成, executedCount=%s", executed_count)
            try:
                await asyncio.wait_for(
                    stop_event.wait(),
                    timeout=settings.app.reconciliation_interval_seconds,
                )
            except TimeoutError:
                pass
    finally:
        await container.close()


def main() -> None:
    """
    启动 Reconciliation Worker

    :return: 无返回值
    """

    asyncio.run(run())
