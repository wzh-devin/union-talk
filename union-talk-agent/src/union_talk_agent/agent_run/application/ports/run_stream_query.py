"""Agent Run SSE 查询端口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/30 10:43
"""

from typing import Protocol

from union_talk_agent.agent_run.domain.agent_run import AgentRun


class RunStreamQuery(Protocol):
    """提供建立 SSE 所需的最小 Run 查询能力。"""

    async def get_run(self, run_id: int) -> AgentRun:
        """
        按 ID 获取 Run

        :param run_id: Agent Run ID
        :return: Agent Run；不存在时返回空
        """

        ...

    async def get_snapshot(self, run_id: int) -> dict[str, object]:
        """
        读取可直接发送给客户端的卡片快照

        :param run_id: Agent Run ID
        :return: Agent 卡片快照；不可用时返回空
        """

        ...
