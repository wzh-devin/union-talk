"""会话消息、资源与上下文扩展召回端口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/12 23:04
"""

from dataclasses import dataclass, field
from datetime import datetime
from typing import Protocol

from union_talk_agent.knowledge.domain.enums import ResourceStatus
from union_talk_agent.knowledge.domain.retrieval_evidence import RetrievalEvidence


@dataclass(frozen=True, slots=True)
class MessageSearchQuery:
    """历史消息检索条件。"""

    conversation_id: int
    query: str
    top_k: int
    participant_user_id_list: list[int] = field(default_factory=lambda: list[int]())
    started_at: datetime | None = None
    ended_at: datetime | None = None


@dataclass(frozen=True, slots=True)
class ResourceSearchQuery:
    """会话资源检索条件。"""

    conversation_id: int
    query: str
    top_k: int
    resource_id_list: list[int] = field(default_factory=lambda: list[int]())
    folder_id: int | None = None
    mime_type_list: list[str] = field(default_factory=lambda: list[str]())


class KnowledgeRetriever(Protocol):
    """按会话隔离执行分类检索与上下文扩展。"""

    async def search_messages(self, search_query: MessageSearchQuery) -> list[RetrievalEvidence]:
        """
        检索会话历史消息

        :param search_query: 历史消息检索条件
        :return: 回表校验和重排后的消息证据
        """

        ...

    async def list_invalid_message_ids(
        self,
        conversation_id: int,
        message_id_list: list[int],
    ) -> set[int]:
        """
        查询不得再次进入模型上下文的消息 ID

        :param conversation_id: 会话 ID
        :param message_id_list: 待检查消息 ID 列表
        :return: 已失效消息 ID 集合
        """

        ...

    async def search_resources(
        self,
        search_query: ResourceSearchQuery,
    ) -> list[RetrievalEvidence]:
        """
        检索会话资源 Child 或 Table Chunk

        :param search_query: 资源检索条件
        :return: 回表校验和重排后的资源证据
        """

        ...

    async def get_resource_status(
        self,
        conversation_id: int,
        resource_id_list: list[int],
    ) -> ResourceStatus | None:
        """
        查询明确引用资源的当前索引状态

        :param conversation_id: 会话 ID
        :param resource_id_list: 资源文件 ID 列表
        :return: 聚合后的当前资源状态；无指定资源时返回空
        """

        ...

    async def expand_resource_context(
        self,
        conversation_id: int,
        chunk_id_list: list[int],
        token_budget: int,
    ) -> list[RetrievalEvidence]:
        """
        加载命中块的 Parent、相邻块和标题路径

        :param conversation_id: 会话 ID
        :param chunk_id_list: 命中 Child Chunk ID 列表
        :param token_budget: 扩展上下文 Token 预算
        :return: 按预算裁剪后的扩展证据
        """

        ...
