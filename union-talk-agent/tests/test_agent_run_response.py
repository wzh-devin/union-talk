"""Agent Run HTTP 响应测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/30 18:10
"""

from datetime import UTC, datetime

from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.interfaces.http.schemas.agent_run_response import AgentRunResponse


def test_run_response_exposes_timeline_timestamps() -> None:
    """
    执行记录响应需要包含排队、开始和完成时间

    :return: 无返回值
    """

    queued_at = datetime(2026, 7, 30, 8, 0, tzinfo=UTC)
    started_at = datetime(2026, 7, 30, 8, 0, 1, tzinfo=UTC)
    completed_at = datetime(2026, 7, 30, 8, 0, 5, tzinfo=UTC)
    run = AgentRun(
        run_id=1,
        conversation_id=2,
        trigger_message_id=3,
        requester_user_id=4,
        binding_id=5,
        binding_version=1,
        agent_id=6,
        agent_version=1,
        credential_id=7,
        credential_version=1,
        credential_owner_user_id=4,
        model_id="deepseek-chat",
        queued_at=queued_at,
        started_at=started_at,
        completed_at=completed_at,
    )

    response = AgentRunResponse.from_domain(run)

    assert response.queued_at == queued_at
    assert response.started_at == started_at
    assert response.completed_at == completed_at
