"""长会话 Token 预算测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:21
"""

from tests.conftest import build_conversation_config
from union_talk_agent.agent_run.application.prompt_assembler import PromptAssembler
from union_talk_agent.agent_run.domain.conversation_context import (
    ConversationContext,
    ConversationMessage,
)
from union_talk_agent.knowledge.domain.enums import SourceType
from union_talk_agent.knowledge.domain.retrieval_evidence import RetrievalEvidence


def test_prompt_uses_recent_messages_without_loading_full_history() -> None:
    """
    只保留预算内的最近消息并排除重复的触发消息

    :return: 无返回值
    """

    context = ConversationContext(
        conversation_id=201,
        trigger_message_id=100,
        question="现在的结论是什么？",
        recent_message_list=[
            ConversationMessage(
                message_id=index,
                sender_type="USER",
                sender_display_name=f"用户{index}",
                content=f"历史内容{index}" * 80,
                created_at_ms=index,
            )
            for index in range(1, 101)
        ],
    )
    evidence_list = [
        RetrievalEvidence(
            source_type=SourceType.RESOURCE_CHUNK,
            source_id=1,
            text_content="资源证据" * 100,
            score=0.9,
        )
    ]
    assembler = PromptAssembler()
    message_list = assembler.assemble(
        build_conversation_config(
            max_context_tokens=1024,
            recent_message_tokens=400,
        ),
        context,
        evidence_list,
    )

    prompt = message_list[1].content
    assert "用户99" in prompt
    assert "用户1：" not in prompt
    assert "用户100：" not in prompt
    assert "当前问题" in prompt
    assert sum(len(message.content) for message in message_list) <= 1024 * 4


def test_prompt_keeps_question_when_history_budget_is_zero() -> None:
    """
    历史预算为零时当前问题仍不可被裁掉

    :return: 无返回值
    """

    context = ConversationContext(
        conversation_id=201,
        trigger_message_id=1,
        question="必须回答的问题",
    )
    message_list = PromptAssembler().assemble(
        build_conversation_config(recent_message_tokens=0),
        context,
        [],
    )

    assert "必须回答的问题" in message_list[1].content


def test_prompt_excludes_legacy_internal_reply_without_dropping_normal_agent_history() -> None:
    """
    过滤历史联调话术，同时保留正常的 Agent 会话上下文

    :return: 无返回值
    """

    context = ConversationContext(
        conversation_id=201,
        trigger_message_id=4,
        question="你是谁？",
        recent_message_list=[
            ConversationMessage(
                message_id=1,
                sender_type="AGENT",
                sender_display_name="AI 助手",
                content="你好，我已经收到你的问题。当前会话 Agent 已接入可靠回复链路。",
                created_at_ms=1,
            ),
            ConversationMessage(
                message_id=2,
                sender_type="AGENT",
                sender_display_name="AI 助手",
                content="上次讨论的预算上限是 32K Token。",
                created_at_ms=2,
            ),
            ConversationMessage(
                message_id=3,
                sender_type="USER",
                sender_display_name="devin",
                content="请结合上次的讨论回答。",
                created_at_ms=3,
            ),
            ConversationMessage(
                message_id=4,
                sender_type="USER",
                sender_display_name="devin",
                content="你是谁？",
                created_at_ms=4,
            ),
        ],
    )

    message_list = PromptAssembler().assemble(
        build_conversation_config(),
        context,
        [],
    )

    assert "可靠回复链路" not in message_list[1].content
    assert "上次讨论的预算上限是 32K Token" in message_list[1].content
    assert "请结合上次的讨论回答" in message_list[1].content
    assert "不要主动汇报" in message_list[0].content
