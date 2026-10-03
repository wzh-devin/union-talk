"""Run 实时事件发布端口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:50
"""

from typing import Protocol

from union_talk_agent.agent_run.realtime.run_event import AgentRunEvent, StoredRunEvent


class RunEventReadError(RuntimeError):
    """Run 实时事件存储暂时不可读。"""


class RunEventReader(Protocol):
    """读取可断线重放的 Run 事件。"""

    async def read_after(
        self,
        run_id: int,
        stream_id: str,
        block_milliseconds: int,
    ) -> list[StoredRunEvent]:
        """
        读取指定 Redis Stream 游标之后的事件

        :param run_id: Agent Run ID
        :param stream_id: Redis Stream 游标
        :param block_milliseconds: Redis Stream 阻塞读取毫秒数
        :return: 指定游标后的实时事件列表
        """

        ...


class RunEventPublisher(RunEventReader, Protocol):
    """发布 Snapshot、Stream 和 Pub/Sub 实时事件。"""

    async def check(self) -> bool:
        """
        检查实时事件存储是否可用

        :return: Redis 实时链路是否就绪
        """

        raise NotImplementedError

    async def publish(self, event: AgentRunEvent) -> AgentRunEvent:
        """
        发布一次 Run 事件

        :param event: 带单调序号的 Run 事件
        :return: 已分配事件 ID 和单调序号的事件
        """

        ...

    async def get_snapshot(self, run_id: int) -> dict[str, object] | None:
        """
        读取 Run 实时快照

        :param run_id: Agent Run ID
        :return: Run 快照，不存在时返回空
        """

        ...
