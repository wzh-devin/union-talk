"""Run 取消快速信号端口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:23
"""

from typing import Protocol


class RunCancellationStore(Protocol):
    """跨进程发布和读取 Run 取消信号。"""

    async def request_cancel(self, run_id: int) -> None:
        """
        写入取消信号

        :param run_id: Agent Run ID
        :return: 无返回值
        """

        ...

    async def is_cancel_requested(self, run_id: int) -> bool:
        """
        检查取消信号

        :param run_id: Agent Run ID
        :return: 是否已经请求取消
        """

        ...
