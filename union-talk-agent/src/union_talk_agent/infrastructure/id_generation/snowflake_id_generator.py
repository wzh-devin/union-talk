"""与 Java Hutool 兼容的雪花 ID 生成器。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:49
"""

import threading
import time


class SnowflakeIdGenerator:
    """生成 64 位分布式业务 ID。"""

    _EPOCH_MILLISECONDS = 1_288_834_974_657
    _WORKER_ID_BITS = 5
    _DATACENTER_ID_BITS = 5
    _SEQUENCE_BITS = 12
    _MAX_NODE_ID = 31
    _MAX_SEQUENCE = (1 << _SEQUENCE_BITS) - 1
    _WORKER_SHIFT = _SEQUENCE_BITS
    _DATACENTER_SHIFT = _SEQUENCE_BITS + _WORKER_ID_BITS
    _TIMESTAMP_SHIFT = _SEQUENCE_BITS + _WORKER_ID_BITS + _DATACENTER_ID_BITS

    def __init__(self, worker_id: int, datacenter_id: int) -> None:
        """
        初始化 SnowflakeIdGenerator

        :param worker_id: 当前 Worker 标识
        :param datacenter_id: Snowflake 数据中心标识
        :return: 无返回值
        """

        if not 0 <= worker_id <= self._MAX_NODE_ID:
            raise ValueError("worker_id 必须在 0 到 31 之间")
        if not 0 <= datacenter_id <= self._MAX_NODE_ID:
            raise ValueError("datacenter_id 必须在 0 到 31 之间")
        self._worker_id = worker_id
        self._datacenter_id = datacenter_id
        self._sequence = 0
        self._last_timestamp = -1
        self._lock = threading.Lock()

    def next_id(self) -> int:
        """
        生成下一个分布式 ID

        :return: 正数 64 位整数
        """

        with self._lock:
            timestamp = self._current_milliseconds()
            if timestamp < self._last_timestamp:
                raise RuntimeError("系统时钟回拨，拒绝生成分布式 ID")
            if timestamp == self._last_timestamp:
                self._sequence = (self._sequence + 1) & self._MAX_SEQUENCE
                if self._sequence == 0:
                    timestamp = self._wait_next_millisecond(self._last_timestamp)
            else:
                self._sequence = 0
            self._last_timestamp = timestamp
            return (
                ((timestamp - self._EPOCH_MILLISECONDS) << self._TIMESTAMP_SHIFT)
                | (self._datacenter_id << self._DATACENTER_SHIFT)
                | (self._worker_id << self._WORKER_SHIFT)
                | self._sequence
            )

    @staticmethod
    def _current_milliseconds() -> int:
        """
        读取当前毫秒时间戳

        :return: 当前毫秒时间戳
        """

        return time.time_ns() // 1_000_000

    def _wait_next_millisecond(self, previous_timestamp: int) -> int:
        """
        等待时钟进入下一毫秒

        :param previous_timestamp: 上一批 ID 使用的毫秒时间戳
        :return: 大于上一批时间戳的新毫秒时间戳
        """

        timestamp = self._current_milliseconds()
        while timestamp <= previous_timestamp:
            time.sleep(0)
            timestamp = self._current_milliseconds()
        return timestamp
