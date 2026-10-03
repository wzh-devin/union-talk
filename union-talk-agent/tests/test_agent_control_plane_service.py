"""Agent 控制面写入不变量测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 19:30
"""

from collections.abc import AsyncIterator
from types import TracebackType
from typing import cast

from tests.test_agent_control_plane_context import (
    build_control_plane_view,
    build_private_permission,
)
from union_talk_agent.agent_config.application.agent_config_service import (
    AgentControlPlaneService,
    ReplaceAgentCredentialCommand,
    SaveAgentDefinitionCommand,
)
from union_talk_agent.agent_config.domain.model_config import RuntimeModelConfig
from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentDefinitionVersionWrite,
    AgentDefinitionWrite,
    AgentUnitOfWorkFactory,
    ConversationAgentBindingWrite,
    ProviderCredentialVersionWrite,
    ProviderCredentialWrite,
)
from union_talk_agent.agent_run.domain.chat_model import (
    ChatMessage,
    ChatModelEvent,
    ChatToolDefinition,
)
from union_talk_agent.agent_run.domain.enums import ProviderEventType
from union_talk_agent.infrastructure.crypto.api_key_cipher import ApiKeyCipher


class SequenceIdGenerator:
    """按给定顺序返回测试业务 ID。"""

    def __init__(self, id_list: list[int]) -> None:
        """
        初始化顺序 ID 生成器

        :param id_list: 按调用顺序返回的 ID 列表
        :return: 无返回值
        """

        self._id_list = id_list

    def next_id(self) -> int:
        """
        返回下一个测试 ID

        :return: 下一个业务 ID
        """

        return self._id_list.pop(0)


class RecordingChatModel:
    """记录连接测试配置且不执行真实模型请求。"""

    def __init__(self) -> None:
        """
        初始化模型调用记录

        :return: 无返回值
        """

        self.tested_api_key_list: list[str] = []

    async def stream(
        self,
        model_config: RuntimeModelConfig,
        message_list: list[ChatMessage],
        max_output_tokens: int,
        temperature: float,
        thinking_enabled: bool,
        thinking_effort: str,
        tool_list: list[ChatToolDefinition] | None = None,
    ) -> AsyncIterator[ChatModelEvent]:
        """
        返回空的模型流

        :param model_config: 未使用的模型配置
        :param message_list: 未使用的消息列表
        :param max_output_tokens: 未使用的输出上限
        :param temperature: 未使用的模型温度
        :param thinking_enabled: 未使用的 Thinking 开关
        :param thinking_effort: 未使用的 Thinking 强度
        :param tool_list: 未使用的工具列表
        :yield: 不产生任何模型增量
        """

        _ignored_arguments = (
            model_config,
            message_list,
            max_output_tokens,
            temperature,
            thinking_enabled,
            thinking_effort,
            tool_list,
        )
        if False:
            yield ChatModelEvent(event_type=ProviderEventType.START)

    async def test_connection(self, model_config: RuntimeModelConfig) -> None:
        """
        记录待测试的 API Key

        :param model_config: 包含 API Key 的运行模型配置
        :return: 无返回值
        """

        self.tested_api_key_list.append(model_config.api_key)


class RecordingBusinessLockStore:
    """记录测试中申请的业务锁。"""

    def __init__(self) -> None:
        """
        初始化业务锁记录

        :return: 无返回值
        """

        self.lock_list: list[tuple[str, str]] = []

    async def lock(self, namespace: str, business_key: str) -> None:
        """
        记录业务锁申请

        :param namespace: 业务锁命名空间
        :param business_key: 业务键
        :return: 无返回值
        """

        self.lock_list.append((namespace, business_key))


class RecordingControlPlaneStore:
    """记录控制面写入且始终返回给定聚合。"""

    def __init__(self) -> None:
        """
        初始化控制面写入记录

        :return: 无返回值
        """

        self.view = build_control_plane_view(credential_owner_user_id=10)
        self.definition_write_list: list[AgentDefinitionWrite] = []
        self.definition_version_write_list: list[AgentDefinitionVersionWrite] = []
        self.credential_write_list: list[ProviderCredentialWrite] = []
        self.credential_version_write_list: list[ProviderCredentialVersionWrite] = []
        self.binding_write_list: list[ConversationAgentBindingWrite] = []

    async def get_view(self, conversation_id: int):
        """
        返回固定控制面聚合

        :param conversation_id: 会话 ID
        :return: 固定控制面聚合
        """

        if conversation_id != self.view.binding.conversation_id:
            raise AssertionError("测试会话ID不匹配")
        return self.view

    async def update_definition(self, record: AgentDefinitionWrite) -> None:
        """
        记录 Agent 定义更新

        :param record: Agent 定义写入数据
        :return: 无返回值
        """

        self.definition_write_list.append(record)

    async def insert_definition_version(self, record: AgentDefinitionVersionWrite) -> None:
        """
        记录 Agent 定义版本新增

        :param record: Agent 定义版本写入数据
        :return: 无返回值
        """

        self.definition_version_write_list.append(record)

    async def update_credential(self, record: ProviderCredentialWrite) -> None:
        """
        记录 Provider 凭证更新

        :param record: Provider 凭证写入数据
        :return: 无返回值
        """

        self.credential_write_list.append(record)

    async def insert_credential(self, record: ProviderCredentialWrite) -> None:
        """
        记录 Provider 凭证新增

        :param record: Provider 凭证写入数据
        :return: 无返回值
        """

        self.credential_write_list.append(record)

    async def insert_credential_version(
        self,
        record: ProviderCredentialVersionWrite,
    ) -> None:
        """
        记录 Provider 凭证版本新增

        :param record: Provider 凭证版本写入数据
        :return: 无返回值
        """

        self.credential_version_write_list.append(record)

    async def update_binding(self, record: ConversationAgentBindingWrite) -> None:
        """
        记录会话绑定更新

        :param record: 会话 Agent 绑定写入数据
        :return: 无返回值
        """

        self.binding_write_list.append(record)


class RecordingUnitOfWork:
    """为控制面服务提供最小事务边界。"""

    def __init__(self, store: RecordingControlPlaneStore) -> None:
        """
        初始化测试工作单元

        :param store: 控制面记录仓储
        :return: 无返回值
        """

        self.agent_control_plane = store
        self.business_locks = RecordingBusinessLockStore()
        self.committed = False

    async def __aenter__(self):
        """
        进入测试事务

        :return: 当前工作单元
        """

        return self

    async def __aexit__(
        self,
        exc_type: type[BaseException] | None,
        exc_value: BaseException | None,
        traceback: TracebackType | None,
    ) -> None:
        """
        退出测试事务

        :param exc_type: 异常类型
        :param exc_value: 异常实例
        :param traceback: 异常堆栈
        :return: 无返回值
        """

    async def commit(self) -> None:
        """
        记录事务提交

        :return: 无返回值
        """

        self.committed = True


class RecordingUnitOfWorkFactory:
    """复用同一记录仓储创建测试工作单元。"""

    def __init__(self, store: RecordingControlPlaneStore) -> None:
        """
        初始化工作单元工厂

        :param store: 控制面记录仓储
        :return: 无返回值
        """

        self._store = store

    def __call__(self) -> RecordingUnitOfWork:
        """
        创建测试工作单元

        :return: 新的测试工作单元
        """

        return RecordingUnitOfWork(self._store)


def build_service(
    store: RecordingControlPlaneStore,
    id_list: list[int],
    chat_model: RecordingChatModel,
) -> AgentControlPlaneService:
    """
    构造使用记录型依赖的 Agent 控制面服务

    :param store: 控制面记录仓储
    :param id_list: 依次返回的业务 ID
    :param chat_model: 记录型聊天模型
    :return: Agent 控制面服务
    """

    factory = cast(
        AgentUnitOfWorkFactory,
        RecordingUnitOfWorkFactory(store),
    )
    return AgentControlPlaneService(
        unit_of_work_factory=factory,
        id_generator=SequenceIdGenerator(id_list),
        api_key_cipher=ApiKeyCipher("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="),
        deepseek_chat_model=chat_model,
    )


def build_definition_command(updated_by: int) -> SaveAgentDefinitionCommand:
    """
    构造现有 Agent 定义更新命令

    :param updated_by: 定义更新用户 ID
    :return: Agent 定义更新命令
    """

    return SaveAgentDefinitionCommand(
        conversation_id=1,
        updated_by=updated_by,
        agent_version=3,
        binding_version=5,
        display_name="共享 AI",
        model_id="deepseek-chat",
        timeout_ms=60_000,
        max_retries=2,
        system_prompt="新的共享指令",
        is_enabled=True,
        is_history_enabled=True,
        is_resource_enabled=True,
        max_context_tokens=32_000,
        max_output_tokens=4096,
        recent_message_tokens=6000,
        message_top_k=20,
        resource_top_k=30,
        temperature=0.2,
        thinking_enabled=True,
        thinking_effort="medium",
    )


async def test_definition_update_does_not_transfer_credential_ownership() -> None:
    """
    验证非赞助者修改共享定义不会更新凭证或改变绑定凭证

    :return: 无返回值
    """

    store = RecordingControlPlaneStore()
    service = build_service(store, [9001], RecordingChatModel())

    await service.save_definition(
        build_definition_command(updated_by=20),
        build_private_permission(user_id=20),
    )

    assert store.credential_write_list == []
    assert len(store.definition_version_write_list) == 1
    binding_write = store.binding_write_list[0]
    assert binding_write.credential_id == 301
    assert binding_write.credential_version == 2
    assert binding_write.updated_by == 20


async def test_successful_new_key_atomically_transfers_current_sponsor() -> None:
    """
    验证另一名私聊成员提交新 Key 后接管当前凭证绑定

    :return: 无返回值
    """

    store = RecordingControlPlaneStore()
    chat_model = RecordingChatModel()
    service = build_service(store, [401, 402], chat_model)

    await service.replace_credential(
        ReplaceAgentCredentialCommand(
            conversation_id=1,
            updated_by=20,
            binding_version=5,
            api_base="https://api.deepseek.com/",
            api_key="sk-new-owner",
        ),
        build_private_permission(user_id=20),
    )

    assert chat_model.tested_api_key_list == ["sk-new-owner"]
    assert len(store.credential_write_list) == 2
    assert store.credential_write_list[0].credential_id == 301
    assert store.credential_write_list[0].status == "DISABLED"
    assert store.credential_write_list[1].credential_id == 401
    assert store.credential_write_list[1].owner_user_id == 20
    assert store.credential_version_write_list[0].record_id == 402
    binding_write = store.binding_write_list[0]
    assert binding_write.credential_id == 401
    assert binding_write.credential_version == 1
    assert binding_write.binding_version == 6
