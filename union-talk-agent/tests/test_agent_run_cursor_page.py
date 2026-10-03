"""Agent Run 游标分页测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/31 15:24
"""

from datetime import UTC, datetime, timedelta

from union_talk_agent.agent_run.application.run_cursor_page import AgentRunCursorPage
from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.interfaces.http.schemas.agent_run_page_response import (
    AgentRunPageResponse,
)


def create_run(run_id: int, queued_at: datetime) -> AgentRun:
    """
    创建游标分页测试 Run

    :param run_id: Agent Run ID
    :param queued_at: 进入队列时间
    :return: Agent Run
    """

    return AgentRun(
        run_id=run_id,
        conversation_id=1,
        trigger_message_id=run_id,
        requester_user_id=2,
        binding_id=3,
        binding_version=1,
        agent_id=4,
        agent_version=1,
        credential_id=5,
        credential_version=1,
        credential_owner_user_id=2,
        model_id="deepseek-chat",
        queued_at=queued_at,
    )


def test_cursor_page_uses_last_visible_run_as_next_cursor() -> None:
    """
    多查一条时使用当前页末项生成下一页双游标

    :return: 无返回值
    """

    now = datetime.now(UTC)
    candidate_list = [
        create_run(300, now),
        create_run(200, now - timedelta(seconds=1)),
        create_run(100, now - timedelta(seconds=2)),
    ]

    page = AgentRunCursorPage.from_candidate_list(candidate_list, page_size=2)

    assert [run.run_id for run in page.run_list] == [300, 200]
    assert page.has_next is True
    assert page.next_cursor_value == candidate_list[1].queued_at
    assert page.next_cursor_id == 200


def test_cursor_page_result_matches_java_field_names() -> None:
    """
    HTTP 游标页字段必须与 Java CursorPageResult 完全一致

    :return: 无返回值
    """

    result = AgentRunPageResponse(
        list=[],
        has_next=True,
        next_cursor_value=datetime(2026, 7, 31, tzinfo=UTC),
        next_cursor_id="100",
    )

    assert result.model_dump(by_alias=True) == {
        "list": [],
        "hasNext": True,
        "nextCursorValue": datetime(2026, 7, 31, tzinfo=UTC),
        "nextCursorId": "100",
    }
