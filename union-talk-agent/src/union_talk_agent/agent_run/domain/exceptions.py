"""Agent Run 领域异常。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from enum import StrEnum


class AgentErrorCode(StrEnum):
    """Agent 对外错误码。"""

    RUN_NOT_FOUND = "RUN_NOT_FOUND"
    RUN_STATE_INVALID = "RUN_STATE_INVALID"
    RUN_CANCELLED = "RUN_CANCELLED"
    TRIGGER_INVALID = "TRIGGER_INVALID"
    CONTEXT_UNAVAILABLE = "CONTEXT_UNAVAILABLE"
    CONFIG_NOT_FOUND = "CONFIG_NOT_FOUND"
    CONFIG_DISABLED = "CONFIG_DISABLED"
    CONFIG_VERSION_CONFLICT = "CONFIG_VERSION_CONFLICT"
    MODEL_REQUEST_FAILED = "MODEL_REQUEST_FAILED"
    MESSAGE_REPLY_FAILED = "MESSAGE_REPLY_FAILED"
    PERMISSION_DENIED = "PERMISSION_DENIED"
    REQUEST_INVALID = "REQUEST_INVALID"
    INFRASTRUCTURE_UNAVAILABLE = "INFRASTRUCTURE_UNAVAILABLE"
    RESOURCE_TOO_LARGE = "RESOURCE_TOO_LARGE"
    RESOURCE_UNSUPPORTED = "RESOURCE_UNSUPPORTED"
    RESOURCE_OCR_REQUIRED = "RESOURCE_OCR_REQUIRED"


class AgentDomainError(Exception):
    """Agent 可识别业务异常。"""

    def __init__(self, code: AgentErrorCode, message: str) -> None:
        """
        初始化 AgentDomainError

        :param code: 错误码或状态码
        :param message: 待发布的消息内容
        :return: 无返回值
        """

        super().__init__(message)
        self.code = code


class RetryableAgentError(AgentDomainError):
    """允许事件重试的 Agent 异常。"""


class NonRetryableAgentError(AgentDomainError):
    """不应重复消费的 Agent 异常。"""


class AgentRunCancelledError(NonRetryableAgentError):
    """Run 已由用户取消。"""
