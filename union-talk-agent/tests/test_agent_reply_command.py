"""Agent 正式回复领域边界测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:37
"""

import pytest

from union_talk_agent.agent_run.domain.agent_citation import AgentCitation
from union_talk_agent.agent_run.domain.commands import CreateAgentReplyCommand
from union_talk_agent.agent_run.domain.constants import (
    MAX_ANSWER_LENGTH,
    MAX_CITATION_COUNT,
    MAX_CITATION_KEY_LENGTH,
)
from union_talk_agent.agent_run.domain.exceptions import AgentDomainError


def build_command(
    *,
    content: str = "回答",
    citation_list: list[AgentCitation] | None = None,
) -> CreateAgentReplyCommand:
    """
    构造写回 Message Service 的命令

    :param content: 待处理的内容
    :param citation_list: Agent 回答引用列表
    :return: 可用于领域校验的 Agent 回复命令
    """

    return CreateAgentReplyCommand(
        run_id=1,
        conversation_id=2,
        trigger_message_id=3,
        reply_to_user_id=5,
        agent_id=4,
        model_id="deepseek-chat",
        content=content,
        citation_list=citation_list or [],
    )


def test_reply_command_accepts_valid_payload() -> None:
    """
    合法回答可以进入 gRPC 防腐层

    :return: 无返回值
    """

    command = build_command(
        citation_list=[
            AgentCitation(
                citation_key="1",
                source_type="RESOURCE_CHUNK",
                asset_file_id=10,
                chunk_id=20,
            )
        ]
    )

    assert command.content == "回答"
    assert len(command.citation_list) == 1


def test_reply_command_rejects_oversized_answer() -> None:
    """
    正式回复正文上限必须由统一常量控制

    :return: 无返回值
    """

    with pytest.raises(AgentDomainError):
        build_command(content="A" * (MAX_ANSWER_LENGTH + 1))


def test_reply_command_rejects_excessive_citations() -> None:
    """
    引用数量超过统一上限时不得调用 Message Service

    :return: 无返回值
    """

    citation = AgentCitation(citation_key="1", source_type="MESSAGE_SEGMENT")
    with pytest.raises(AgentDomainError):
        build_command(citation_list=[citation] * (MAX_CITATION_COUNT + 1))


def test_citation_rejects_oversized_key() -> None:
    """
    引用标识长度由领域常量统一维护

    :return: 无返回值
    """

    with pytest.raises(AgentDomainError):
        AgentCitation(
            citation_key="A" * (MAX_CITATION_KEY_LENGTH + 1),
            source_type="MESSAGE_SEGMENT",
        )
