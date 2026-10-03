"""重建 Agent 定义、凭证、会话绑定和 Run 快照结构。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/08/07 19:20
"""

import sqlalchemy as sa

from alembic import op
from union_talk_agent.infrastructure.postgres.tables import metadata

revision = "20260807_0003"
down_revision = "20260731_0002"
branch_labels = None
depends_on = None


def upgrade() -> None:
    """
    使用开发期一次性替换迁移创建 Agent 控制面 V3

    :return: 无返回值
    """

    for table_name in (
        "ut_agent_run",
        "ut_agent_conversation_config",
        "ut_agent_provider_config",
    ):
        op.drop_table(table_name, schema="agent", if_exists=True)
    metadata.create_all(bind=op.get_bind(), checkfirst=True)


def downgrade() -> None:
    """
    删除 V3 控制面并恢复开发期旧组合配置表

    :return: 无返回值
    """

    for table_name in (
        "ut_agent_run",
        "ut_agent_conversation_binding",
        "ut_agent_provider_credential_version",
        "ut_agent_provider_credential",
        "ut_agent_definition_version",
        "ut_agent_definition",
    ):
        op.drop_table(table_name, schema="agent", if_exists=True)

    op.create_table(
        "ut_agent_provider_config",
        sa.Column("id", sa.BigInteger, comment="Provider配置ID"),
        sa.Column("scope_type", sa.String(16), comment="配置作用域类型"),
        sa.Column("scope_id", sa.BigInteger, comment="配置作用域ID"),
        sa.Column("provider", sa.String(32), comment="模型Provider"),
        sa.Column("api_base", sa.String(512), comment="Provider API地址"),
        sa.Column("api_key_ciphertext", sa.LargeBinary, comment="API Key密文"),
        sa.Column("api_key_nonce", sa.LargeBinary, comment="API Key加密随机数"),
        sa.Column("key_fingerprint", sa.String(32), comment="API Key指纹"),
        sa.Column("model_id", sa.String(128), comment="模型标识"),
        sa.Column("timeout_ms", sa.Integer, comment="请求超时毫秒数"),
        sa.Column("max_retries", sa.SmallInteger, comment="首Token前最大重试次数"),
        sa.Column("version", sa.Integer, comment="配置版本"),
        sa.Column("last_test_status", sa.String(16), comment="最近连接测试状态"),
        sa.Column("last_test_error", sa.String(500), comment="最近连接测试错误摘要"),
        sa.Column("tested_at", sa.DateTime(timezone=True), comment="最近连接测试时间"),
        sa.Column("created_by", sa.BigInteger, comment="创建用户ID"),
        sa.Column("updated_by", sa.BigInteger, comment="更新用户ID"),
        sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
        sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
        schema="agent",
        comment="Agent模型Provider配置",
    )
    op.create_table(
        "ut_agent_conversation_config",
        sa.Column("id", sa.BigInteger, comment="会话Agent配置ID"),
        sa.Column("conversation_id", sa.BigInteger, comment="会话ID"),
        sa.Column("provider_config_id", sa.BigInteger, comment="Provider配置ID"),
        sa.Column("display_name", sa.String(128), comment="Agent显示名称"),
        sa.Column("system_prompt", sa.Text, comment="会话系统指令"),
        sa.Column("enabled", sa.Boolean, comment="是否启用Agent"),
        sa.Column("history_enabled", sa.Boolean, comment="是否启用历史消息"),
        sa.Column("resource_enabled", sa.Boolean, comment="是否启用资源检索"),
        sa.Column("max_context_tokens", sa.Integer, comment="最大上下文Token数"),
        sa.Column("max_output_tokens", sa.Integer, comment="最大输出Token数"),
        sa.Column("recent_message_tokens", sa.Integer, comment="近期消息Token预算"),
        sa.Column("message_top_k", sa.SmallInteger, comment="消息召回数量"),
        sa.Column("resource_top_k", sa.SmallInteger, comment="资源召回数量"),
        sa.Column("temperature", sa.Numeric(3, 2), comment="模型温度"),
        sa.Column("config_version", sa.Integer, comment="会话配置版本"),
        sa.Column("index_status", sa.String(16), comment="会话索引状态"),
        sa.Column("created_by", sa.BigInteger, comment="创建用户ID"),
        sa.Column("updated_by", sa.BigInteger, comment="更新用户ID"),
        sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
        sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
        schema="agent",
        comment="会话Agent配置",
    )
    op.create_table(
        "ut_agent_run",
        sa.Column("id", sa.BigInteger, comment="Agent Run ID"),
        sa.Column("conversation_id", sa.BigInteger, comment="会话ID"),
        sa.Column("trigger_message_id", sa.BigInteger, comment="触发消息ID"),
        sa.Column("requester_user_id", sa.BigInteger, comment="请求用户ID"),
        sa.Column("agent_config_id", sa.BigInteger, comment="会话Agent配置ID"),
        sa.Column("config_version", sa.Integer, comment="配置快照版本"),
        sa.Column("status", sa.String(16), comment="Run状态"),
        sa.Column("stage", sa.String(16), comment="Run阶段"),
        sa.Column("answer_message_id", sa.BigInteger, comment="正式回复消息ID"),
        sa.Column("model_id", sa.String(128), comment="模型标识快照"),
        sa.Column("input_tokens", sa.Integer, comment="输入Token数量"),
        sa.Column("output_tokens", sa.Integer, comment="输出Token数量"),
        sa.Column("first_token_latency_ms", sa.Integer, comment="首Token延迟毫秒数"),
        sa.Column("total_latency_ms", sa.Integer, comment="总耗时毫秒数"),
        sa.Column("last_event_sequence", sa.BigInteger, comment="实时事件最新序号"),
        sa.Column("worker_id", sa.String(128), comment="当前执行Worker标识"),
        sa.Column("lease_expires_at", sa.DateTime(timezone=True), comment="执行租约到期时间"),
        sa.Column("heartbeat_at", sa.DateTime(timezone=True), comment="最近执行心跳时间"),
        sa.Column("first_token_at", sa.DateTime(timezone=True), comment="首Token时间"),
        sa.Column("cancel_requested_at", sa.DateTime(timezone=True), comment="取消请求时间"),
        sa.Column("cancel_requested_by", sa.BigInteger, comment="取消请求用户ID"),
        sa.Column("trace_schema_version", sa.SmallInteger, comment="轨迹Schema版本"),
        sa.Column("error_code", sa.String(64), comment="失败错误码"),
        sa.Column("error_message", sa.String(500), comment="失败错误摘要"),
        sa.Column("queued_at", sa.DateTime(timezone=True), comment="进入队列时间"),
        sa.Column("started_at", sa.DateTime(timezone=True), comment="开始执行时间"),
        sa.Column("completed_at", sa.DateTime(timezone=True), comment="完成时间"),
        sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
        sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
        schema="agent",
        comment="Agent单次运行",
    )
    op.create_index(
        "idx_agent_provider_config_scope",
        "ut_agent_provider_config",
        ["scope_type", "scope_id"],
        schema="agent",
    )
    op.create_index(
        "idx_agent_conversation_config_conversation",
        "ut_agent_conversation_config",
        ["conversation_id"],
        schema="agent",
    )
    op.create_index(
        "idx_agent_run_conversation_queued_id",
        "ut_agent_run",
        ["conversation_id", "queued_at", "id"],
        schema="agent",
    )
