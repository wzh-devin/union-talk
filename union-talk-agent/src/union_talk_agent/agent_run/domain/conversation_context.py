"""Agent 会话上下文模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from dataclasses import dataclass, field


@dataclass(frozen=True, slots=True)
class ConversationMessage:
    """供回答使用的紧凑会话消息。"""

    message_id: int
    sender_type: str
    sender_display_name: str
    content: str
    created_at_ms: int


@dataclass(frozen=True, slots=True)
class ConversationContext:
    """Message Service 返回的当前问题和近期上下文。"""

    conversation_id: int
    trigger_message_id: int
    question: str
    recent_message_list: list[ConversationMessage] = field(
        default_factory=lambda: list[ConversationMessage]()
    )
    receiver_user_id_list: list[int] = field(default_factory=lambda: list[int]())
    quoted_message_id: int | None = None
    referenced_resource_id_list: list[int] = field(default_factory=lambda: list[int]())
    referenced_asset_file_id_list: list[int] = field(default_factory=lambda: list[int]())
