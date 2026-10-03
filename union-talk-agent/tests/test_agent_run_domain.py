"""Agent Run 状态机测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:21
"""

import pytest

from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.agent_run.domain.enums import AgentRunStage, AgentRunStatus
from union_talk_agent.agent_run.domain.exceptions import AgentDomainError


def build_run() -> AgentRun:
    """
    构造待执行 Run

    :return: 待执行的测试 Agent Run
    """

    return AgentRun.create(
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
        model_id="fixed-answer",
    )


def test_run_success_state_machine() -> None:
    """
    Run 只允许从运行中进入成功终态

    :return: 无返回值
    """

    run = build_run()
    run.start()
    run.change_stage(AgentRunStage.GENERATING)
    run.succeed(9)

    assert run.status is AgentRunStatus.SUCCEEDED
    assert run.answer_message_id == 9
    assert run.is_terminal


def test_retryable_run_can_be_requeued() -> None:
    """
    可重试失败释放为 QUEUED 并保留错误摘要

    :return: 无返回值
    """

    run = build_run()
    run.start()
    run.requeue("TEMPORARY", "暂时失败")

    assert run.status is AgentRunStatus.QUEUED
    assert run.stage is AgentRunStage.PREPARING
    assert run.error_code == "TEMPORARY"


def test_success_run_cannot_be_failed_again() -> None:
    """
    成功 Run 不允许被迟到的 Worker 覆盖为失败

    :return: 无返回值
    """

    run = build_run()
    run.start()
    run.succeed(9)

    with pytest.raises(AgentDomainError):
        run.fail("LATE", "迟到错误")


def test_cancel_records_requester_and_terminal_status() -> None:
    """
    取消必须记录请求用户并进入不可认领终态

    :return: 无返回值
    """

    run = build_run()
    run.start()
    run.cancel(requested_by=88)

    assert run.status is AgentRunStatus.CANCELLED
    assert run.cancel_requested_by == 88
    assert run.cancel_requested_at is not None
    assert run.is_terminal
