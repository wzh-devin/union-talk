"""Agent 独立 SSE 生命周期测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:40
"""

import pytest

from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.agent_run.realtime.run_event import StoredRunEvent
from union_talk_agent.interfaces.http.routers.agent_run_router import stream_run_events


class TerminalSnapshotQuery:
    """返回已完成的卡片快照。"""

    def __init__(self) -> None:
        """
        初始化 TerminalSnapshotQuery

        :return: 无返回值
        """

        self.get_run_call_count = 0
        self.get_snapshot_call_count = 0

    async def get_snapshot(self, run_id: int) -> dict[str, object]:
        """
        返回终态快照

        :param run_id: Agent Run ID
        :return: Agent 卡片快照；不可用时返回空
        """

        self.get_snapshot_call_count += 1
        return {
            "runId": str(run_id),
            "status": "SUCCEEDED",
            "lastSequence": 9,
        }

    async def get_run(self, run_id: int) -> AgentRun:
        """
        终态快照不应再触发数据库 Run 查询

        :param run_id: Agent Run ID
        :return: Agent Run；不存在时返回空
        """

        self.get_run_call_count += 1
        raise AssertionError(f"终态SSE不应查询Run, runId={run_id}")


class UnexpectedEventPublisher:
    """终态快照后不应再读取 Redis Stream。"""

    def __init__(self) -> None:
        """
        初始化 UnexpectedEventPublisher

        :return: 无返回值
        """

        self.read_call_count = 0

    async def read_after(
        self,
        run_id: int,
        stream_id: str,
        block_milliseconds: int,
    ) -> list[StoredRunEvent]:
        """
        一旦调用就令测试失败

        :param run_id: Agent Run ID
        :param stream_id: Redis Stream 游标
        :param block_milliseconds: Redis Stream 阻塞读取毫秒数
        :return: 指定游标后的实时事件列表
        """

        self.read_call_count += 1
        raise AssertionError(
            "终态SSE不应继续阻塞读取, "
            f"runId={run_id}, streamId={stream_id}, blockMs={block_milliseconds}"
        )


async def test_terminal_snapshot_closes_sse_stream() -> None:
    """
    首次连接读到终态快照后应立即关闭 SSE

    :return: 无返回值
    """

    query = TerminalSnapshotQuery()
    event_reader = UnexpectedEventPublisher()
    stream = stream_run_events(100, query, event_reader, None)

    first_frame = await anext(stream)
    assert "event: snapshot" in first_frame
    with pytest.raises(StopAsyncIteration):
        await anext(stream)
    assert query.get_run_call_count == 0
    assert query.get_snapshot_call_count == 1
    assert event_reader.read_call_count == 0


async def test_resume_still_sends_snapshot_before_stream_replay() -> None:
    """
    重连携带游标时仍先返回最新 Snapshot

    :return: 无返回值
    """

    query = TerminalSnapshotQuery()
    event_reader = UnexpectedEventPublisher()
    stream = stream_run_events(100, query, event_reader, "1723500000000-0")

    first_frame = await anext(stream)
    assert "event: snapshot" in first_frame
    with pytest.raises(StopAsyncIteration):
        await anext(stream)
    assert query.get_snapshot_call_count == 1
    assert query.get_run_call_count == 0
