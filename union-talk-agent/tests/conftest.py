"""测试共享构造器。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 19:21
"""

from union_talk_agent.agent_config.domain.agent_config import ConversationAgentConfig


def build_conversation_config(
    *,
    max_context_tokens: int = 4096,
    recent_message_tokens: int = 1024,
) -> ConversationAgentConfig:
    """
    构造可按测试覆盖预算的会话配置

    :param max_context_tokens: 最大上下文 Token 数
    :param recent_message_tokens: 最近消息 Token 预算
    :return: 测试使用的会话 Agent 配置
    """

    return ConversationAgentConfig(
        agent_id=101,
        agent_version=1,
        display_name="AI",
        system_prompt="你是会话助手。",
        is_history_enabled=True,
        is_resource_enabled=True,
        max_context_tokens=max_context_tokens,
        max_output_tokens=1024,
        recent_message_tokens=recent_message_tokens,
        message_top_k=20,
        resource_top_k=30,
        temperature=0.2,
        thinking_enabled=False,
        thinking_effort="medium",
    )
