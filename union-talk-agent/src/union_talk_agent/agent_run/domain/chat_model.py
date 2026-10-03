"""聊天模型边界数据。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from dataclasses import dataclass, field
from typing import Any

from union_talk_agent.agent_run.domain.enums import ProviderEventType


@dataclass(frozen=True, slots=True)
class ChatToolDefinition:
    """提供给模型的结构化工具定义。"""

    name: str
    description: str
    parameters: dict[str, Any]


@dataclass(frozen=True, slots=True)
class ChatToolCall:
    """模型请求执行的一次完整工具调用。"""

    call_id: str
    name: str
    arguments: str


@dataclass(frozen=True, slots=True)
class ChatToolCallDelta:
    """模型流式返回的工具调用增量。"""

    index: int
    call_id: str = ""
    name: str = ""
    arguments_delta: str = ""


@dataclass(frozen=True, slots=True)
class ChatMessage:
    """发送给模型的单条消息。"""

    role: str
    content: str
    reasoning_content: str = ""
    tool_call_list: tuple[ChatToolCall, ...] = field(default_factory=tuple)
    tool_call_id: str | None = None
    name: str | None = None


@dataclass(frozen=True, slots=True)
class ChatModelEvent:
    """模型 Provider 输出的判别联合流式事件。"""

    event_type: ProviderEventType
    content_index: int | None = None
    delta: str = ""
    finish_reason: str | None = None
    tool_call_delta: ChatToolCallDelta | None = None
    input_tokens: int = 0
    output_tokens: int = 0
    error_code: str | None = None
    error_message: str | None = None


@dataclass(frozen=True, slots=True)
class AgentReplyResult:
    """Message Service 正式回复写回结果。"""

    answer_message_id: int
    is_created: bool
