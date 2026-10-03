"""基于 Token 预算组装会话与 RAG 上下文。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:59
"""

from union_talk_agent.agent_config.domain.agent_config import ConversationAgentConfig
from union_talk_agent.agent_run.domain.chat_model import ChatMessage
from union_talk_agent.agent_run.domain.constants import (
    DEFAULT_AGENT_SYSTEM_PROMPT,
    LEGACY_INTERNAL_AGENT_REPLY_MARKERS,
)
from union_talk_agent.agent_run.domain.conversation_context import ConversationContext
from union_talk_agent.knowledge.domain.retrieval_evidence import RetrievalEvidence


class PromptAssembler:
    """按优先级裁剪输入，避免将完整长会话直接送入模型。"""

    _CHARS_PER_TOKEN = 4
    _MIN_QUESTION_TOKENS = 256
    _MAX_SINGLE_MESSAGE_CHARS = 4000
    _MAX_SINGLE_EVIDENCE_CHARS = 6000

    def assemble(
        self,
        config: ConversationAgentConfig,
        context: ConversationContext,
        evidence_list: list[RetrievalEvidence],
    ) -> list[ChatMessage]:
        """
        组装有硬预算上限的模型消息列表

        优先级依次为系统指令、当前问题、近期消息、检索证据。旧消息只通过
        消息片段检索进入上下文，不扫描完整历史。

        :param config: 会话 Agent 配置
        :param context: Agent 会话上下文
        :param evidence_list: 知识证据列表
        :return: 符合上下文预算的模型消息列表
        """

        custom_system_prompt = config.system_prompt.strip()
        system_prompt = DEFAULT_AGENT_SYSTEM_PROMPT
        if custom_system_prompt:
            system_prompt = (
                f"{DEFAULT_AGENT_SYSTEM_PROMPT}\n\n会话管理员补充指令：\n{custom_system_prompt}"
            )
        question = context.question.strip()
        fixed_token_cost = self._estimate_tokens(system_prompt) + max(
            self._estimate_tokens(question),
            self._MIN_QUESTION_TOKENS,
        )
        remaining_tokens = max(config.max_context_tokens - fixed_token_cost, 0)
        history_budget = min(config.recent_message_tokens, remaining_tokens)
        history_text = self._select_recent_messages(context, history_budget)
        remaining_tokens -= self._estimate_tokens(history_text)
        evidence_text = self._select_evidence(evidence_list, remaining_tokens)

        user_section_list = [f"当前问题：\n{question}"]
        if history_text:
            user_section_list.insert(0, f"近期会话：\n{history_text}")
        if evidence_text:
            user_section_list.insert(1, f"检索证据：\n{evidence_text}")
        return [
            ChatMessage(role="system", content=system_prompt),
            ChatMessage(role="user", content="\n\n".join(user_section_list)),
        ]

    def _select_recent_messages(
        self,
        context: ConversationContext,
        token_budget: int,
    ) -> str:
        """
        按 Token 预算选择最近消息

        :param context: Agent 会话上下文
        :param token_budget: 可用 Token 预算
        :return: Token 预算内的最近消息列表
        """

        selected_reversed: list[str] = []
        used_tokens = 0
        for message in reversed(context.recent_message_list):
            if message.message_id == context.trigger_message_id:
                continue
            if message.sender_type.upper() == "AGENT" and any(
                marker in message.content for marker in LEGACY_INTERNAL_AGENT_REPLY_MARKERS
            ):
                continue
            content = message.content[: self._MAX_SINGLE_MESSAGE_CHARS]
            line = f"{message.sender_display_name}：{content}"
            line_tokens = self._estimate_tokens(line)
            if used_tokens + line_tokens > token_budget:
                continue
            selected_reversed.append(line)
            used_tokens += line_tokens
        return "\n".join(reversed(selected_reversed))

    def _select_evidence(
        self,
        evidence_list: list[RetrievalEvidence],
        token_budget: int,
    ) -> str:
        """
        按 Token 预算选择知识证据

        :param evidence_list: 知识证据列表
        :param token_budget: 可用 Token 预算
        :return: Token 预算内的证据列表
        """

        selected: list[str] = []
        used_tokens = 0
        for index, evidence in enumerate(evidence_list, start=1):
            content = evidence.text_content[: self._MAX_SINGLE_EVIDENCE_CHARS]
            block = f"[{index}] {content}"
            block_tokens = self._estimate_tokens(block)
            if used_tokens + block_tokens > token_budget:
                continue
            selected.append(block)
            used_tokens += block_tokens
        return "\n\n".join(selected)

    @classmethod
    def _estimate_tokens(cls, text: str) -> int:
        """
        估算文本 Token 数量

        :param text: 待处理文本
        :return: 估算的 Token 数量
        """

        return max((len(text) + cls._CHARS_PER_TOKEN - 1) // cls._CHARS_PER_TOKEN, 1)
