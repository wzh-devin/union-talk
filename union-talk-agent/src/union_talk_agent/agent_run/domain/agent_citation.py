"""Agent 回答引用模型。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:46
"""

from dataclasses import dataclass

from union_talk_agent.agent_run.domain.constants import (
    MAX_CITATION_KEY_LENGTH,
    MAX_HEADING_PATH_LENGTH,
)
from union_talk_agent.agent_run.domain.exceptions import AgentDomainError, AgentErrorCode


@dataclass(frozen=True, slots=True)
class AgentCitation:
    """经过白名单校验的回答引用。"""

    citation_key: str
    source_type: str
    message_id: int | None = None
    asset_file_id: int | None = None
    resource_version: int | None = None
    chunk_id: int | None = None
    page_from: int | None = None
    page_to: int | None = None
    heading_path: str = ""

    def __post_init__(self) -> None:
        """
        校验引用进入应用层后的稳定业务边界

        :return: 无返回值
        """

        if not 1 <= len(self.citation_key) <= MAX_CITATION_KEY_LENGTH:
            raise AgentDomainError(
                AgentErrorCode.REQUEST_INVALID,
                "引用标识长度非法",
            )
        if len(self.heading_path) > MAX_HEADING_PATH_LENGTH:
            raise AgentDomainError(
                AgentErrorCode.REQUEST_INVALID,
                "引用标题路径过长",
            )
