"""分布式 ID 生成端口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:50
"""

from typing import Protocol


class IdGenerator(Protocol):
    """生成全局业务 ID。"""

    def next_id(self) -> int:
        """
        生成下一个 ID

        :return: 正数 64 位整数
        """

        ...
