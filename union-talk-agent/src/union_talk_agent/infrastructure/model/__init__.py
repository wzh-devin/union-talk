"""模型 Provider 适配器。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:58
"""

from union_talk_agent.infrastructure.model.deepseek_chat_model import DeepSeekChatModel
from union_talk_agent.infrastructure.model.fixed_answer_chat_model import FixedAnswerChatModel

__all__ = ["DeepSeekChatModel", "FixedAnswerChatModel"]
