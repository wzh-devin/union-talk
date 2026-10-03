"""Agent PostgreSQL Unit of Work 端口。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:50
"""

from dataclasses import dataclass
from datetime import datetime
from types import TracebackType
from typing import Protocol, Self

from union_talk_agent.agent_config.domain.agent_config import AgentControlPlaneView
from union_talk_agent.agent_run.domain.agent_run import AgentRun
from union_talk_agent.agent_run.domain.agent_runtime import (
    AgentOpenRuntime,
    AgentRunContentBlock,
    AgentRunMessage,
    AgentRunTurn,
    AgentToolExecution,
)
from union_talk_agent.agent_run.domain.agent_step import AgentStep
from union_talk_agent.event_inbox.domain.inbox_event import InboxEvent
from union_talk_agent.knowledge.domain.asset_lifecycle import AssetLifecycle
from union_talk_agent.knowledge.domain.enums import ResourceStatus
from union_talk_agent.knowledge.domain.indexed_resource import IndexedResource
from union_talk_agent.knowledge.domain.message_segment import MessageSegment
from union_talk_agent.knowledge.domain.resource_chunk import ResourceChunk
from union_talk_agent.knowledge.domain.retrieval_evidence import (
    RetrievalEvidence,
    VectorCandidate,
)


class BusinessLockStore(Protocol):
    """PostgreSQL 事务级业务锁。"""

    async def lock(self, namespace: str, business_key: str) -> None:
        """
        获取事务级 advisory lock

        :param namespace: 稳定业务命名空间
        :param business_key: 业务键
        :return: 无返回值
        """

        ...


class InboxEventStore(Protocol):
    """Inbox 幂等记录数据访问。"""

    async def get_by_event_id(self, event_id: str) -> InboxEvent | None:
        """
        按事件 ID 查询记录

        :param event_id: 事件 ID
        :return: Inbox 事件记录，不存在时返回空
        """

        ...

    async def insert(self, inbox_event: InboxEvent) -> None:
        """
        新增 Inbox 记录

        :param inbox_event: Inbox 事件记录
        :return: 无返回值
        """

        ...

    async def update(self, inbox_event: InboxEvent) -> None:
        """
        更新 Inbox 状态

        :param inbox_event: Inbox 事件记录
        :return: 无返回值
        """

        ...


class AgentRunStore(Protocol):
    """Agent Run 数据访问。"""

    async def get_by_id(self, run_id: int) -> AgentRun | None:
        """
        按 ID 查询 Run

        :param run_id: Agent Run ID
        :return: Agent Run，不存在时返回空
        """

        ...

    async def get_by_trigger_message_id(self, trigger_message_id: int) -> AgentRun | None:
        """
        按触发消息查询 Run

        :param trigger_message_id: 触发消息 ID
        :return: 触发消息对应的 Agent Run，不存在时返回空
        """

        ...

    async def insert(self, agent_run: AgentRun) -> None:
        """
        新增 Run

        :param agent_run: 待处理的 Agent Run
        :return: 无返回值
        """

        ...

    async def update(self, agent_run: AgentRun) -> None:
        """
        更新 Run

        :param agent_run: 待处理的 Agent Run
        :return: 无返回值
        """

        ...

    async def next_event_sequence(self, run_id: int) -> int:
        """
        原子递增并返回实时事件序号

        :param run_id: Agent Run ID
        :return: 递增后的实时事件序号
        """

        ...

    async def claim_execution(
        self,
        run_id: int,
        worker_id: str,
        lease_seconds: int,
    ) -> AgentRun | None:
        """
        原子认领待执行或租约过期的 Run

        :param run_id: Agent Run ID
        :param worker_id: 当前 Worker 标识
        :param lease_seconds: 执行租约有效秒数
        :return: 认领成功的 Agent Run；有效租约被占用时返回空
        """

        ...

    async def renew_execution_lease(
        self,
        run_id: int,
        worker_id: str,
        lease_seconds: int,
    ) -> bool:
        """
        续租当前 Worker 已认领的 Run

        :param run_id: Agent Run ID
        :param worker_id: 当前 Worker 标识
        :param lease_seconds: 执行租约有效秒数
        :return: 判断结果
        """

        ...

    async def list_active_by_conversation_id(self, conversation_id: int) -> list[AgentRun]:
        """
        查询会话活动 Run

        :param conversation_id: 会话 ID
        :return: 当前会话排队中和运行中的 Agent Run 列表
        """

        ...

    async def list_by_conversation_id(
        self,
        conversation_id: int,
        page_size: int,
        cursor_value: datetime | None,
        cursor_id: int | None,
    ) -> list[AgentRun]:
        """
        按双游标查询会话 Agent Run

        :param conversation_id: 会话 ID
        :param page_size: 当前页展示数量
        :param cursor_value: 上一页末项排队时间
        :param cursor_id: 上一页末项 Run ID
        :return: 多查一条且按排队时间和 Run ID 倒序排列的 Agent Run 列表
        """

        ...

    async def list_reconcilable(self, batch_size: int) -> list[AgentRun]:
        """
        认领需要对账的 Run

        :param batch_size: 单批处理数量上限
        :return: 需要对账修复的 Agent Run 列表
        """

        ...


class AgentControlPlaneStore(Protocol):
    """Agent 定义、凭证和会话绑定数据访问。"""

    async def get_view(self, conversation_id: int) -> AgentControlPlaneView | None:
        """
        查询会话当前控制面聚合

        :param conversation_id: 会话 ID
        :return: 当前控制面聚合，不存在时返回空
        """

        ...

    async def get_view_by_agent(
        self,
        conversation_id: int,
        agent_id: int,
    ) -> AgentControlPlaneView | None:
        """
        按会话和稳定 Agent ID 查询当前控制面聚合

        :param conversation_id: 会话 ID
        :param agent_id: 稳定 Agent ID
        :return: 匹配的控制面聚合，不存在时返回空
        """

        ...

    async def get_versioned_view(
        self,
        *,
        binding_id: int,
        binding_version: int,
        agent_id: int,
        agent_version: int,
        credential_id: int,
        credential_version: int,
    ) -> AgentControlPlaneView | None:
        """
        按 Run 快照版本查询控制面聚合

        :param binding_id: 会话绑定 ID
        :param binding_version: 会话绑定版本
        :param agent_id: Agent 定义 ID
        :param agent_version: Agent 定义版本
        :param credential_id: Provider 凭证 ID
        :param credential_version: Provider 凭证版本
        :return: 完整版本聚合，不存在时返回空
        """

        ...

    async def insert_definition(self, record: "AgentDefinitionWrite") -> None:
        """
        新增 Agent 稳定定义

        :param record: Agent 定义写入数据
        :return: 无返回值
        """

        ...

    async def update_definition(self, record: "AgentDefinitionWrite") -> None:
        """
        更新 Agent 稳定定义

        :param record: Agent 定义写入数据
        :return: 无返回值
        """

        ...

    async def insert_definition_version(self, record: "AgentDefinitionVersionWrite") -> None:
        """
        新增 Agent 定义版本

        :param record: Agent 定义版本写入数据
        :return: 无返回值
        """

        ...

    async def insert_credential(self, record: "ProviderCredentialWrite") -> None:
        """
        新增 Provider 凭证身份

        :param record: Provider 凭证写入数据
        :return: 无返回值
        """

        ...

    async def update_credential(self, record: "ProviderCredentialWrite") -> None:
        """
        更新 Provider 凭证身份

        :param record: Provider 凭证写入数据
        :return: 无返回值
        """

        ...

    async def insert_credential_version(
        self,
        record: "ProviderCredentialVersionWrite",
    ) -> None:
        """
        新增 Provider 凭证密文版本

        :param record: Provider 凭证版本写入数据
        :return: 无返回值
        """

        ...

    async def update_connection_test(
        self,
        credential_id: int,
        credential_version: int,
        status: str,
        error_message: str | None,
    ) -> None:
        """
        更新指定凭证版本的连接测试结果

        :param credential_id: Provider 凭证 ID
        :param credential_version: Provider 凭证版本
        :param status: 连接测试状态
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        ...

    async def insert_binding(self, record: "ConversationAgentBindingWrite") -> None:
        """
        新增会话 Agent 绑定

        :param record: 会话绑定写入数据
        :return: 无返回值
        """

        ...

    async def update_binding(self, record: "ConversationAgentBindingWrite") -> None:
        """
        更新会话 Agent 绑定

        :param record: 会话绑定写入数据
        :return: 无返回值
        """

        ...


class AgentTraceStore(Protocol):
    """Agent Run 结构化轨迹数据访问。"""

    async def insert_step(self, agent_step: AgentStep) -> None:
        """
        新增运行步骤

        :param agent_step: 待处理的 Agent 执行步骤
        :return: 无返回值
        """

        ...

    async def update_step(
        self,
        agent_step: AgentStep,
        input_summary: dict[str, object] | None,
        output_summary: dict[str, object] | None,
    ) -> None:
        """
        更新运行步骤

        :param agent_step: 待处理的 Agent 执行步骤
        :param input_summary: 脱敏输入摘要
        :param output_summary: 脱敏输出摘要
        :return: 无返回值
        """

        ...

    async def list_by_run_id(self, run_id: int) -> list[dict[str, object]]:
        """
        查询 Run 可展示步骤

        :param run_id: Agent Run ID
        :return: 成员可见的结构化执行步骤列表
        """

        ...

    async def insert_turn(self, turn: AgentRunTurn, message: AgentRunMessage) -> None:
        """
        新增 Turn 和对应 partial assistant message

        :param turn: Agent Turn
        :param message: partial assistant message
        :return: 无返回值
        """

        ...

    async def update_turn(self, turn: AgentRunTurn, message: AgentRunMessage) -> None:
        """
        更新 Turn 和运行时消息终态

        :param turn: 已完成或失败的 Agent Turn
        :param message: 已完成或失败的运行时消息
        :return: 无返回值
        """

        ...

    async def insert_content_block(self, block: AgentRunContentBlock) -> None:
        """
        在内容块结束时写入完整正文

        :param block: 已结束的完整内容块
        :return: 无返回值
        """

        ...

    async def insert_tool_execution(self, execution: AgentToolExecution) -> None:
        """
        新增工具执行轨迹

        :param execution: 运行中的工具执行
        :return: 无返回值
        """

        ...

    async def update_tool_execution(self, execution: AgentToolExecution) -> None:
        """
        更新工具执行终态和摘要

        :param execution: 已完成或失败的工具执行
        :return: 无返回值
        """

        ...

    async def get_runtime_trace(self, run_id: int) -> dict[str, object]:
        """
        查询按 Turn 和 Message 聚合的 V2 执行轨迹

        :param run_id: Agent Run ID
        :return: Turn、Message、Content Block 和工具执行聚合
        """

        ...

    async def get_latest_turn_no(self, run_id: int) -> int:
        """
        查询 Run 已创建的最大 Turn 序号

        :param run_id: Agent Run ID
        :return: 最大 Turn 序号，不存在时返回零
        """

        ...

    async def list_open_runtime(self, run_id: int) -> list[AgentOpenRuntime]:
        """
        查询租约过期后仍未关闭的 Turn 和 Message

        :param run_id: Agent Run ID
        :return: 未关闭运行时列表
        """

        ...

    async def interrupt_runtime(
        self,
        runtime: AgentOpenRuntime,
        partial_block_list: list[AgentRunContentBlock],
    ) -> None:
        """
        关闭旧运行时并保存 Redis 中最后的 partial block

        :param runtime: 待关闭运行时
        :param partial_block_list: 尚未落库的 partial block 列表
        :return: 无返回值
        """

        ...


class KnowledgeEvidenceStore(Protocol):
    """知识证据回表数据访问。"""

    async def load_ready_evidence(
        self,
        conversation_id: int,
        candidate_list: list[VectorCandidate],
    ) -> list[RetrievalEvidence]:
        """
        加载当前会话可用证据

        :param conversation_id: 会话 ID
        :param candidate_list: 向量召回候选列表
        :return: 通过会话、版本和状态校验的证据列表
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

    async def expand_resource_context(
        self,
        conversation_id: int,
        chunk_id_list: list[int],
    ) -> list[RetrievalEvidence]:
        """
        加载命中块的 Parent 和相邻资源块

        :param conversation_id: 会话 ID
        :param chunk_id_list: 命中资源块 ID 列表
        :return: 已校验当前版本的扩展证据
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
        :return: 聚合后的当前资源状态；无记录时返回空
        """

        ...


class KnowledgeIndexStore(Protocol):
    """资源索引元数据访问。"""

    async def get_by_asset_version(
        self,
        asset_file_id: int,
        resource_version: int,
    ) -> IndexedResource | None:
        """
        按资产和版本查询资源索引

        :param asset_file_id: 资源文件 ID
        :param resource_version: 资源版本
        :return: 指定版本的索引资源，不存在时返回空
        """

        ...

    async def replace_prepared_resource(
        self,
        resource: IndexedResource,
        chunk_list: list[ResourceChunk],
        ingest_job_id: int,
        worker_id: str,
    ) -> None:
        """
        写入 INDEXING 资源、任务和新切块

        :param resource: 资源索引领域对象
        :param chunk_list: 资源切块列表
        :param ingest_job_id: 资源入库任务 ID
        :param worker_id: 当前 Worker 标识
        :return: 无返回值
        """

        ...

    async def mark_ready(
        self,
        resource: IndexedResource,
        ingest_job_id: int,
    ) -> None:
        """
        将资源、向量和任务切换为 READY/SUCCEEDED

        :param resource: 资源索引领域对象
        :param ingest_job_id: 资源入库任务 ID
        :return: 无返回值
        """

        ...

    async def mark_failed(
        self,
        resource_id: int,
        ingest_job_id: int,
        error_code: str,
        error_message: str,
    ) -> None:
        """
        记录资源入库失败

        :param resource_id: 资源索引 ID
        :param ingest_job_id: 资源入库任务 ID
        :param error_code: 错误码
        :param error_message: 脱敏错误摘要
        :return: 无返回值
        """

        ...

    async def mark_deleted(self, asset_file_id: int) -> None:
        """
        使资产全部资源版本与切块失效

        :param asset_file_id: 资产文件 ID
        :return: 无返回值
        """

        ...


class AssetLifecycleStore(Protocol):
    """File Service 资产生命周期投影访问。"""

    async def get_by_asset_file_id(self, asset_file_id: int) -> AssetLifecycle | None:
        """
        查询资产最新生命周期事实

        :param asset_file_id: 资产文件 ID
        :return: 资产生命周期事实，不存在时返回空
        """

        ...

    async def save(self, lifecycle: AssetLifecycle) -> None:
        """
        新增或更新资产生命周期事实

        :param lifecycle: 资产生命周期事实
        :return: 无返回值
        """

        ...


class MessageSegmentStore(Protocol):
    """会话消息检索片段数据访问。"""

    async def get_by_message_id(self, message_id: int) -> MessageSegment | None:
        """
        按消息 ID 查询片段

        :param message_id: 消息 ID
        :return: 消息片段，不存在时返回空
        """

        ...

    async def save_pending(self, segment: MessageSegment) -> None:
        """
        新增或覆盖待向量化片段

        :param segment: 消息检索片段
        :return: 无返回值
        """

        ...

    async def mark_ready(self, segment: MessageSegment) -> None:
        """
        将片段标记为可检索

        :param segment: 消息检索片段
        :return: 无返回值
        """

        ...

    async def mark_failed(self, segment_id: int) -> None:
        """
        将片段标记为失败

        :param segment_id: 消息片段 ID
        :return: 无返回值
        """

        ...

    async def mark_deleted(self, segment_id: int) -> None:
        """
        将消息片段标记为不可检索

        :param segment_id: 消息片段 ID
        :return: 无返回值
        """

        ...

    async def invalidate_by_message_id(self, message_id: int) -> None:
        """
        使指定消息片段失效

        :param message_id: 消息 ID
        :return: 无返回值
        """

        ...

    async def invalidate_by_asset_file_id(self, asset_file_id: int) -> list[int]:
        """
        使引用指定资产的消息片段失效

        :param asset_file_id: 已删除资产文件 ID
        :return: 已失效的消息 ID 列表
        """

        ...

    async def get_asset_version_map_by_message_id_list(
        self,
        message_id_list: list[int],
    ) -> dict[int, int]:
        """
        查询来源消息已经继承的资产血缘

        :param message_id_list: 来源消息 ID 列表
        :return: 资产文件 ID 与资源版本映射
        """

        ...


class AgentUnitOfWork(Protocol):
    """Agent 事务边界。"""

    business_locks: BusinessLockStore
    inbox_events: InboxEventStore
    agent_runs: AgentRunStore
    agent_control_plane: AgentControlPlaneStore
    agent_traces: AgentTraceStore
    knowledge_evidence: KnowledgeEvidenceStore
    knowledge_index: KnowledgeIndexStore
    asset_lifecycles: AssetLifecycleStore
    message_segments: MessageSegmentStore

    async def __aenter__(self) -> Self:
        """
        开启数据库事务

        :return: 当前工作单元
        """

        ...

    async def __aexit__(
        self,
        exc_type: type[BaseException] | None,
        exc_value: BaseException | None,
        traceback: TracebackType | None,
    ) -> None:
        """
        提交前异常时回滚并关闭 Session

        :param exc_type: 上下文退出时的异常类型
        :param exc_value: 上下文退出时的异常实例
        :param traceback: 上下文退出时的异常堆栈
        :return: 无返回值
        """

        ...

    async def commit(self) -> None:
        """
        提交当前事务

        :return: 无返回值
        """

        ...


class AgentUnitOfWorkFactory(Protocol):
    """创建请求级 Agent Unit of Work。"""

    def __call__(self) -> AgentUnitOfWork:
        """
        创建新的 Unit of Work

        :return: 尚未进入事务的 Unit of Work
        """

        ...


@dataclass(frozen=True, slots=True)
class AgentDefinitionWrite:
    """Agent 稳定定义写入数据。"""

    agent_id: int
    owner_type: str
    owner_id: int
    display_name: str
    status: str
    latest_version: int
    updated_by: int
    now: datetime


@dataclass(frozen=True, slots=True)
class AgentDefinitionVersionWrite:
    """Agent 不可变定义版本写入数据。"""

    record_id: int
    agent_id: int
    version: int
    model_id: str
    timeout_ms: int
    max_retries: int
    system_prompt: str
    is_history_enabled: bool
    is_resource_enabled: bool
    max_context_tokens: int
    max_output_tokens: int
    recent_message_tokens: int
    message_top_k: int
    resource_top_k: int
    temperature: float
    thinking_enabled: bool
    thinking_effort: str
    created_by: int
    now: datetime


@dataclass(frozen=True, slots=True)
class ProviderCredentialWrite:
    """Provider 凭证身份写入数据。"""

    credential_id: int
    owner_user_id: int
    usage_scope_type: str
    usage_scope_id: int
    provider: str
    status: str
    latest_version: int
    updated_by: int
    now: datetime


@dataclass(frozen=True, slots=True)
class ProviderCredentialVersionWrite:
    """Provider 不可变凭证版本写入数据。"""

    record_id: int
    credential_id: int
    version: int
    api_base: str
    api_key_ciphertext: bytes
    api_key_nonce: bytes
    key_fingerprint: str
    connection_test_status: str
    connection_test_error: str | None
    tested_at: datetime | None
    created_by: int
    now: datetime


@dataclass(frozen=True, slots=True)
class ConversationAgentBindingWrite:
    """会话 Agent 绑定写入数据。"""

    binding_id: int
    conversation_id: int
    agent_id: int
    agent_version: int
    credential_id: int | None
    credential_version: int | None
    status: str
    binding_version: int
    updated_by: int
    now: datetime
