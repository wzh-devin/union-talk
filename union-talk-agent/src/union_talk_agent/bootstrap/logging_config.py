"""进程日志配置。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:07
"""

import logging


def configure_logging(level: str) -> None:
    """
    配置不包含敏感载荷的基础结构化友好日志

    :param level: 日志级别
    :return: 无返回值
    """

    logging.basicConfig(
        level=level.upper(),
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)
