"""异步 Worker 停止信号。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:08
"""

import asyncio
import signal


def install_stop_signal_handlers(stop_event: asyncio.Event) -> None:
    """
    将 SIGINT/SIGTERM 转换为协作式停止事件

    :param stop_event: 进程协作式停止事件
    :return: 无返回值
    """

    loop = asyncio.get_running_loop()

    def request_stop(_signal_number: signal.Signals) -> None:
        """
        设置进程协作式停止事件

        :param _signal_number: 触发停止处理的系统信号
        :return: 无返回值
        """

        stop_event.set()

    for signal_number in (signal.SIGINT, signal.SIGTERM):
        try:
            loop.add_signal_handler(signal_number, request_stop, signal_number)
        except NotImplementedError:
            pass
