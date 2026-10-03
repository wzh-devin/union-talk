"""Agent Run 字符串枚举。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from enum import StrEnum


class AgentRunStatus(StrEnum):
    """Agent Run 状态。"""

    QUEUED = "QUEUED"
    RUNNING = "RUNNING"
    SUCCEEDED = "SUCCEEDED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


class AgentRunStage(StrEnum):
    """Agent Run 当前阶段。"""

    PREPARING = "PREPARING"
    RETRIEVING = "RETRIEVING"
    TOOL_RUNNING = "TOOL_RUNNING"
    GENERATING = "GENERATING"
    FINALIZING = "FINALIZING"


class AgentStepType(StrEnum):
    """Agent 执行步骤类型。"""

    VALIDATION = "VALIDATION"
    CONTEXT_LOAD = "CONTEXT_LOAD"
    RETRIEVAL = "RETRIEVAL"
    RERANK = "RERANK"
    TOOL_CALL = "TOOL_CALL"
    MODEL_DECISION = "MODEL_DECISION"
    MODEL_GENERATION = "MODEL_GENERATION"
    CITATION_VALIDATION = "CITATION_VALIDATION"
    MESSAGE_PERSIST = "MESSAGE_PERSIST"


class AgentStepStatus(StrEnum):
    """Agent 执行步骤状态。"""

    PENDING = "PENDING"
    RUNNING = "RUNNING"
    SUCCEEDED = "SUCCEEDED"
    FAILED = "FAILED"
    SKIPPED = "SKIPPED"
    CANCELLED = "CANCELLED"


class TraceVisibility(StrEnum):
    """执行轨迹可见范围。"""

    MEMBER = "MEMBER"
    ADMIN = "ADMIN"
    INTERNAL = "INTERNAL"


class AgentRealtimeEventType(StrEnum):
    """Agent 实时事件类型。"""

    AGENT_START = "agent_start"
    TURN_START = "turn_start"
    MESSAGE_START = "message_start"
    MESSAGE_UPDATE = "message_update"
    MESSAGE_END = "message_end"
    TOOL_EXECUTION_START = "tool_execution_start"
    TOOL_EXECUTION_UPDATE = "tool_execution_update"
    TOOL_EXECUTION_END = "tool_execution_end"
    TURN_END = "turn_end"
    AGENT_END = "agent_end"


class ProviderEventType(StrEnum):
    """模型 Provider 流式事件类型。"""

    START = "start"
    TEXT_START = "text_start"
    TEXT_DELTA = "text_delta"
    TEXT_END = "text_end"
    THINKING_START = "thinking_start"
    THINKING_DELTA = "thinking_delta"
    THINKING_END = "thinking_end"
    TOOL_CALL_START = "toolcall_start"
    TOOL_CALL_DELTA = "toolcall_delta"
    TOOL_CALL_END = "toolcall_end"
    DONE = "done"
    ERROR = "error"


class RetrievalToolResultStatus(StrEnum):
    """检索工具结果状态。"""

    READY = "READY"
    EMPTY = "EMPTY"
    INDEXING = "INDEXING"
    FAILED = "FAILED"


class AgentTurnStatus(StrEnum):
    """Agent Turn 状态。"""

    RUNNING = "RUNNING"
    SUCCEEDED = "SUCCEEDED"
    FAILED = "FAILED"
    INTERRUPTED = "INTERRUPTED"


class AgentMessageStatus(StrEnum):
    """Agent 运行时消息状态。"""

    STREAMING = "STREAMING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    INTERRUPTED = "INTERRUPTED"


class AgentContentBlockType(StrEnum):
    """Assistant Message 内容块类型。"""

    TEXT = "TEXT"
    THINKING = "THINKING"
    TOOL_CALL = "TOOL_CALL"


class AgentContentBlockStatus(StrEnum):
    """Assistant Message 内容块状态。"""

    STREAMING = "STREAMING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    INTERRUPTED = "INTERRUPTED"


class AgentToolExecutionStatus(StrEnum):
    """Agent 工具执行状态。"""

    RUNNING = "RUNNING"
    SUCCEEDED = "SUCCEEDED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"
    INTERRUPTED = "INTERRUPTED"


class MessageSenderType(StrEnum):
    """消息发送者类型。"""

    USER = "USER"
    AGENT = "AGENT"
    SYSTEM = "SYSTEM"
