"""Agent Run HTTP 响应。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:05
"""

from datetime import datetime

from pydantic import BaseModel, ConfigDict
from pydantic.alias_generators import to_camel

from union_talk_agent.agent_run.domain.agent_run import AgentRun


class AgentRunResponse(BaseModel):
    """单次 Agent Run 的安全状态视图。"""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    run_id: str
    conversation_id: str
    trigger_message_id: str
    requester_user_id: str
    binding_id: str
    binding_version: int
    agent_id: str
    agent_version: int
    credential_id: str
    credential_version: int
    credential_owner_user_id: str
    model_id: str
    status: str
    stage: str
    answer_message_id: str | None
    last_event_sequence: int
    error_code: str | None
    error_message: str | None
    queued_at: datetime | None
    started_at: datetime | None
    completed_at: datetime | None

    @classmethod
    def from_domain(cls, run: AgentRun) -> "AgentRunResponse":
        """
        从 Run 聚合构造响应

        :param run: 当前运行聚合
        :return: 对应的响应模型
        """

        return cls(
            run_id=str(run.run_id),
            conversation_id=str(run.conversation_id),
            trigger_message_id=str(run.trigger_message_id),
            requester_user_id=str(run.requester_user_id),
            binding_id=str(run.binding_id),
            binding_version=run.binding_version,
            agent_id=str(run.agent_id),
            agent_version=run.agent_version,
            credential_id=str(run.credential_id),
            credential_version=run.credential_version,
            credential_owner_user_id=str(run.credential_owner_user_id),
            model_id=run.model_id,
            status=str(run.status),
            stage=str(run.stage),
            answer_message_id=(
                str(run.answer_message_id) if run.answer_message_id is not None else None
            ),
            last_event_sequence=run.last_event_sequence,
            error_code=run.error_code,
            error_message=run.error_message,
            queued_at=run.queued_at,
            started_at=run.started_at,
            completed_at=run.completed_at,
        )
