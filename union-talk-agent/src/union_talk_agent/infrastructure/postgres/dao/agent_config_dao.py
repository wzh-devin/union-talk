"""Agent 控制面 DAO。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 18:35
"""

from collections.abc import Mapping
from datetime import UTC, datetime
from decimal import Decimal
from typing import Any

import sqlalchemy as sa
from sqlalchemy.engine import RowMapping
from sqlalchemy.ext.asyncio import AsyncSession

from union_talk_agent.agent_config.domain.agent_config import (
    AgentControlPlaneView,
    AgentDefinition,
    AgentDefinitionVersion,
    ConversationAgentBinding,
    ProviderCredential,
    ProviderCredentialVersion,
)
from union_talk_agent.agent_config.domain.enums import (
    AgentBindingStatus,
    AgentDefinitionStatus,
    AgentOwnerType,
    ConnectionTestStatus,
    CredentialStatus,
    CredentialUsageScopeType,
    ProviderType,
)
from union_talk_agent.agent_run.application.ports.agent_unit_of_work import (
    AgentDefinitionVersionWrite,
    AgentDefinitionWrite,
    ConversationAgentBindingWrite,
    ProviderCredentialVersionWrite,
    ProviderCredentialWrite,
)
from union_talk_agent.infrastructure.postgres.column_expression import table_column
from union_talk_agent.infrastructure.postgres.query_cardinality import select_one_or_none
from union_talk_agent.infrastructure.postgres.tables import (
    agent_conversation_binding_table,
    agent_definition_table,
    agent_definition_version_table,
    agent_provider_credential_table,
    agent_provider_credential_version_table,
)


class AgentControlPlaneDao:
    """持久化 Agent 定义、凭证和会话绑定。"""

    def __init__(self, session: AsyncSession) -> None:
        """
        初始化 AgentControlPlaneDao

        :param session: 异步数据库会话
        :return: 无返回值
        """

        self._session = session

    async def get_view(self, conversation_id: int) -> AgentControlPlaneView | None:
        """
        查询会话当前控制面聚合

        :param conversation_id: 会话 ID
        :return: 当前控制面聚合，不存在时返回空
        """

        binding_row = await self._select_binding(conversation_id)
        if binding_row is None:
            return None
        return await self._load_view(
            binding_row,
            int(binding_row["agent_version"]),
            (
                int(binding_row["credential_version"])
                if binding_row["credential_version"] is not None
                else None
            ),
        )

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

        row = await select_one_or_none(
            self._session,
            sa.select(agent_conversation_binding_table)
            .where(
                table_column(agent_conversation_binding_table, "conversation_id") == conversation_id
            )
            .where(table_column(agent_conversation_binding_table, "agent_id") == agent_id)
            .order_by(agent_conversation_binding_table.c["updated_at"].desc()),
            "同一会话和Agent存在重复绑定",
        )
        if row is None:
            return None
        return await self._load_view(
            row,
            int(row["agent_version"]),
            int(row["credential_version"]) if row["credential_version"] is not None else None,
        )

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

        row = await select_one_or_none(
            self._session,
            sa.select(agent_conversation_binding_table)
            .where(table_column(agent_conversation_binding_table, "id") == binding_id)
            .where(
                table_column(agent_conversation_binding_table, "binding_version") >= binding_version
            ),
            "同一绑定ID存在重复记录",
        )
        if row is None or int(row["agent_id"]) != agent_id:
            return None
        row_map = dict(row)
        row_map["agent_version"] = agent_version
        row_map["credential_id"] = credential_id
        row_map["credential_version"] = credential_version
        row_map["binding_version"] = binding_version
        return await self._load_view(row_map, agent_version, credential_version)

    async def insert_definition(self, record: AgentDefinitionWrite) -> None:
        """
        新增 Agent 稳定定义

        :param record: Agent 定义写入数据
        :return: 无返回值
        """

        await self._session.execute(
            sa.insert(agent_definition_table).values(
                id=record.agent_id,
                owner_type=record.owner_type,
                owner_id=record.owner_id,
                display_name=record.display_name,
                status=record.status,
                latest_version=record.latest_version,
                created_by=record.updated_by,
                updated_by=record.updated_by,
                created_at=record.now,
                updated_at=record.now,
            )
        )

    async def update_definition(self, record: AgentDefinitionWrite) -> None:
        """
        更新 Agent 稳定定义

        :param record: Agent 定义写入数据
        :return: 无返回值
        """

        await self._session.execute(
            sa.update(agent_definition_table)
            .where(table_column(agent_definition_table, "id") == record.agent_id)
            .values(
                display_name=record.display_name,
                status=record.status,
                latest_version=record.latest_version,
                updated_by=record.updated_by,
                updated_at=record.now,
            )
        )

    async def insert_definition_version(self, record: AgentDefinitionVersionWrite) -> None:
        """
        新增 Agent 定义版本

        :param record: Agent 定义版本写入数据
        :return: 无返回值
        """

        await self._session.execute(
            sa.insert(agent_definition_version_table).values(
                id=record.record_id,
                agent_id=record.agent_id,
                version=record.version,
                model_id=record.model_id,
                timeout_ms=record.timeout_ms,
                max_retries=record.max_retries,
                system_prompt=record.system_prompt,
                history_enabled=record.is_history_enabled,
                resource_enabled=record.is_resource_enabled,
                max_context_tokens=record.max_context_tokens,
                max_output_tokens=record.max_output_tokens,
                recent_message_tokens=record.recent_message_tokens,
                message_top_k=record.message_top_k,
                resource_top_k=record.resource_top_k,
                temperature=record.temperature,
                thinking_enabled=record.thinking_enabled,
                thinking_effort=record.thinking_effort,
                created_by=record.created_by,
                created_at=record.now,
            )
        )

    async def insert_credential(self, record: ProviderCredentialWrite) -> None:
        """
        新增 Provider 凭证身份

        :param record: Provider 凭证写入数据
        :return: 无返回值
        """

        await self._session.execute(
            sa.insert(agent_provider_credential_table).values(
                id=record.credential_id,
                owner_user_id=record.owner_user_id,
                usage_scope_type=record.usage_scope_type,
                usage_scope_id=record.usage_scope_id,
                provider=record.provider,
                status=record.status,
                latest_version=record.latest_version,
                created_by=record.updated_by,
                updated_by=record.updated_by,
                created_at=record.now,
                updated_at=record.now,
            )
        )

    async def update_credential(self, record: ProviderCredentialWrite) -> None:
        """
        更新 Provider 凭证身份

        :param record: Provider 凭证写入数据
        :return: 无返回值
        """

        await self._session.execute(
            sa.update(agent_provider_credential_table)
            .where(table_column(agent_provider_credential_table, "id") == record.credential_id)
            .values(
                status=record.status,
                latest_version=record.latest_version,
                updated_by=record.updated_by,
                updated_at=record.now,
            )
        )

    async def insert_credential_version(
        self,
        record: ProviderCredentialVersionWrite,
    ) -> None:
        """
        新增 Provider 凭证密文版本

        :param record: Provider 凭证版本写入数据
        :return: 无返回值
        """

        await self._session.execute(
            sa.insert(agent_provider_credential_version_table).values(
                id=record.record_id,
                credential_id=record.credential_id,
                version=record.version,
                api_base=record.api_base,
                api_key_ciphertext=record.api_key_ciphertext,
                api_key_nonce=record.api_key_nonce,
                key_fingerprint=record.key_fingerprint,
                connection_test_status=record.connection_test_status,
                connection_test_error=record.connection_test_error,
                tested_at=record.tested_at,
                created_by=record.created_by,
                created_at=record.now,
            )
        )

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

        await self._session.execute(
            sa.update(agent_provider_credential_version_table)
            .where(
                table_column(agent_provider_credential_version_table, "credential_id")
                == credential_id
            )
            .where(
                table_column(agent_provider_credential_version_table, "version")
                == credential_version
            )
            .values(
                connection_test_status=status,
                connection_test_error=error_message,
                tested_at=datetime.now(UTC),
            )
        )

    async def insert_binding(self, record: ConversationAgentBindingWrite) -> None:
        """
        新增会话 Agent 绑定

        :param record: 会话绑定写入数据
        :return: 无返回值
        """

        await self._session.execute(
            sa.insert(agent_conversation_binding_table).values(
                id=record.binding_id,
                conversation_id=record.conversation_id,
                agent_id=record.agent_id,
                agent_version=record.agent_version,
                credential_id=record.credential_id,
                credential_version=record.credential_version,
                status=record.status,
                binding_version=record.binding_version,
                created_by=record.updated_by,
                updated_by=record.updated_by,
                created_at=record.now,
                updated_at=record.now,
            )
        )

    async def update_binding(self, record: ConversationAgentBindingWrite) -> None:
        """
        更新会话 Agent 绑定

        :param record: 会话绑定写入数据
        :return: 无返回值
        """

        await self._session.execute(
            sa.update(agent_conversation_binding_table)
            .where(table_column(agent_conversation_binding_table, "id") == record.binding_id)
            .values(
                agent_id=record.agent_id,
                agent_version=record.agent_version,
                credential_id=record.credential_id,
                credential_version=record.credential_version,
                status=record.status,
                binding_version=record.binding_version,
                updated_by=record.updated_by,
                updated_at=record.now,
            )
        )

    async def _select_binding(self, conversation_id: int) -> RowMapping | None:
        """
        查询会话当前绑定行

        :param conversation_id: 会话 ID
        :return: 当前绑定数据库行，不存在时返回空
        """

        return await select_one_or_none(
            self._session,
            sa.select(agent_conversation_binding_table)
            .where(
                table_column(agent_conversation_binding_table, "conversation_id") == conversation_id
            )
            .order_by(agent_conversation_binding_table.c["updated_at"].desc()),
            "同一会话存在重复Agent绑定",
        )

    async def _load_view(
        self,
        binding_row: Mapping[Any, Any],
        agent_version: int,
        credential_version: int | None,
    ) -> AgentControlPlaneView | None:
        """
        加载绑定所指向的定义和凭证版本

        :param binding_row: 会话绑定数据库行
        :param agent_version: 指定 Agent 定义版本
        :param credential_version: 指定 Provider 凭证版本
        :return: 完整控制面聚合，关键记录缺失时返回空
        """

        agent_id = int(binding_row["agent_id"])
        definition_row = await select_one_or_none(
            self._session,
            sa.select(agent_definition_table).where(
                table_column(agent_definition_table, "id") == agent_id
            ),
            "同一Agent定义ID存在重复记录",
        )
        version_row = await select_one_or_none(
            self._session,
            sa.select(agent_definition_version_table)
            .where(table_column(agent_definition_version_table, "agent_id") == agent_id)
            .where(table_column(agent_definition_version_table, "version") == agent_version),
            "同一Agent定义版本存在重复记录",
        )
        if definition_row is None or version_row is None:
            return None

        credential_id_value = binding_row["credential_id"]
        credential: ProviderCredential | None = None
        credential_record: ProviderCredentialVersion | None = None
        if credential_id_value is not None and credential_version is not None:
            credential_id = int(credential_id_value)
            credential_row = await select_one_or_none(
                self._session,
                sa.select(agent_provider_credential_table).where(
                    table_column(agent_provider_credential_table, "id") == credential_id
                ),
                "同一Provider凭证ID存在重复记录",
            )
            secret_row = await select_one_or_none(
                self._session,
                sa.select(agent_provider_credential_version_table)
                .where(
                    table_column(agent_provider_credential_version_table, "credential_id")
                    == credential_id
                )
                .where(
                    table_column(agent_provider_credential_version_table, "version")
                    == credential_version
                ),
                "同一Provider凭证版本存在重复记录",
            )
            if credential_row is None or secret_row is None:
                return None
            credential = self._to_credential(credential_row)
            credential_record = self._to_credential_version(secret_row)

        return AgentControlPlaneView(
            binding=self._to_binding(binding_row),
            definition=self._to_definition(definition_row),
            definition_version=self._to_definition_version(version_row),
            credential=credential,
            credential_version=credential_record,
        )

    @staticmethod
    def _to_definition(row: RowMapping) -> AgentDefinition:
        """
        将数据库行转换为 Agent 稳定定义

        :param row: 数据库查询行
        :return: Agent 稳定定义
        """

        return AgentDefinition(
            agent_id=int(row["id"]),
            owner_type=AgentOwnerType(str(row["owner_type"])),
            owner_id=int(row["owner_id"]),
            display_name=str(row["display_name"]),
            status=AgentDefinitionStatus(str(row["status"])),
            latest_version=int(row["latest_version"]),
        )

    @staticmethod
    def _to_definition_version(row: RowMapping) -> AgentDefinitionVersion:
        """
        将数据库行转换为 Agent 定义版本

        :param row: 数据库查询行
        :return: Agent 定义版本
        """

        temperature = row["temperature"]
        return AgentDefinitionVersion(
            agent_id=int(row["agent_id"]),
            version=int(row["version"]),
            model_id=str(row["model_id"]),
            timeout_ms=int(row["timeout_ms"]),
            max_retries=int(row["max_retries"]),
            system_prompt=str(row["system_prompt"] or ""),
            is_history_enabled=bool(row["history_enabled"]),
            is_resource_enabled=bool(row["resource_enabled"]),
            max_context_tokens=int(row["max_context_tokens"]),
            max_output_tokens=int(row["max_output_tokens"]),
            recent_message_tokens=int(row["recent_message_tokens"]),
            message_top_k=int(row["message_top_k"]),
            resource_top_k=int(row["resource_top_k"]),
            temperature=float(
                temperature if isinstance(temperature, Decimal) else temperature or 0.2
            ),
            thinking_enabled=bool(row["thinking_enabled"]),
            thinking_effort=str(row["thinking_effort"] or "medium"),
        )

    @staticmethod
    def _to_credential(row: RowMapping) -> ProviderCredential:
        """
        将数据库行转换为 Provider 凭证身份

        :param row: 数据库查询行
        :return: Provider 凭证身份
        """

        return ProviderCredential(
            credential_id=int(row["id"]),
            owner_user_id=int(row["owner_user_id"]),
            usage_scope_type=CredentialUsageScopeType(str(row["usage_scope_type"])),
            usage_scope_id=int(row["usage_scope_id"]),
            provider=ProviderType(str(row["provider"])),
            status=CredentialStatus(str(row["status"])),
            latest_version=int(row["latest_version"]),
        )

    @staticmethod
    def _to_credential_version(row: RowMapping) -> ProviderCredentialVersion:
        """
        将数据库行转换为 Provider 凭证版本

        :param row: 数据库查询行
        :return: Provider 凭证版本
        """

        return ProviderCredentialVersion(
            credential_id=int(row["credential_id"]),
            version=int(row["version"]),
            api_base=str(row["api_base"]),
            api_key_ciphertext=bytes(row["api_key_ciphertext"]),
            api_key_nonce=bytes(row["api_key_nonce"]),
            key_fingerprint=str(row["key_fingerprint"]),
            connection_test_status=ConnectionTestStatus(str(row["connection_test_status"])),
            connection_test_error=(
                str(row["connection_test_error"])
                if row["connection_test_error"] is not None
                else None
            ),
            tested_at=row["tested_at"],
        )

    @staticmethod
    def _to_binding(
        row: Mapping[Any, Any],
    ) -> ConversationAgentBinding:
        """
        将数据库行转换为会话 Agent 绑定

        :param row: 数据库查询行
        :return: 会话 Agent 绑定
        """

        return ConversationAgentBinding(
            binding_id=int(row["id"]),
            conversation_id=int(row["conversation_id"]),
            agent_id=int(row["agent_id"]),
            agent_version=int(row["agent_version"]),
            credential_id=(int(row["credential_id"]) if row["credential_id"] is not None else None),
            credential_version=(
                int(row["credential_version"]) if row["credential_version"] is not None else None
            ),
            status=AgentBindingStatus(str(row["status"])),
            binding_version=int(row["binding_version"]),
            updated_by=int(row["updated_by"]),
        )
