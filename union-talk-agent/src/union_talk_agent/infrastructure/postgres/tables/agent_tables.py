"""Agent schema 的 SQLAlchemy Core Table 定义。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/29 18:49
"""

import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

metadata = sa.MetaData(schema="agent")

agent_definition_table = sa.Table(
    "ut_agent_definition",
    metadata,
    sa.Column("id", sa.BigInteger, comment="Agent定义ID"),
    sa.Column("owner_type", sa.String(16), comment="Agent所有者类型"),
    sa.Column("owner_id", sa.BigInteger, comment="Agent所有者业务ID"),
    sa.Column("display_name", sa.String(128), comment="Agent显示名称"),
    sa.Column("status", sa.String(16), comment="Agent定义状态"),
    sa.Column("latest_version", sa.Integer, comment="Agent最新版本"),
    sa.Column("created_by", sa.BigInteger, comment="创建用户ID"),
    sa.Column("updated_by", sa.BigInteger, comment="更新用户ID"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="Agent稳定定义",
)

agent_definition_version_table = sa.Table(
    "ut_agent_definition_version",
    metadata,
    sa.Column("id", sa.BigInteger, comment="Agent定义版本记录ID"),
    sa.Column("agent_id", sa.BigInteger, comment="Agent定义ID"),
    sa.Column("version", sa.Integer, comment="Agent定义版本"),
    sa.Column("model_id", sa.String(128), comment="模型标识"),
    sa.Column("timeout_ms", sa.Integer, comment="请求超时毫秒数"),
    sa.Column("max_retries", sa.SmallInteger, comment="首Token前最大重试次数"),
    sa.Column("system_prompt", sa.Text, comment="系统指令"),
    sa.Column("history_enabled", sa.Boolean, comment="是否启用历史消息"),
    sa.Column("resource_enabled", sa.Boolean, comment="是否启用资源检索"),
    sa.Column("max_context_tokens", sa.Integer, comment="最大上下文Token数"),
    sa.Column("max_output_tokens", sa.Integer, comment="最大输出Token数"),
    sa.Column("recent_message_tokens", sa.Integer, comment="近期消息Token预算"),
    sa.Column("message_top_k", sa.SmallInteger, comment="消息召回数量"),
    sa.Column("resource_top_k", sa.SmallInteger, comment="资源召回数量"),
    sa.Column("temperature", sa.Numeric(3, 2), comment="模型温度"),
    sa.Column("thinking_enabled", sa.Boolean, comment="是否启用模型Thinking"),
    sa.Column("thinking_effort", sa.String(16), comment="模型Thinking强度"),
    sa.Column("created_by", sa.BigInteger, comment="创建用户ID"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    comment="Agent不可变定义版本",
)

agent_provider_credential_table = sa.Table(
    "ut_agent_provider_credential",
    metadata,
    sa.Column("id", sa.BigInteger, comment="Provider凭证ID"),
    sa.Column("owner_user_id", sa.BigInteger, comment="凭证所有者用户ID"),
    sa.Column("usage_scope_type", sa.String(16), comment="凭证使用作用域类型"),
    sa.Column("usage_scope_id", sa.BigInteger, comment="凭证使用作用域ID"),
    sa.Column("provider", sa.String(32), comment="模型Provider"),
    sa.Column("status", sa.String(16), comment="Provider凭证状态"),
    sa.Column("latest_version", sa.Integer, comment="凭证最新版本"),
    sa.Column("created_by", sa.BigInteger, comment="创建用户ID"),
    sa.Column("updated_by", sa.BigInteger, comment="更新用户ID"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="Provider凭证身份与所有权",
)

agent_provider_credential_version_table = sa.Table(
    "ut_agent_provider_credential_version",
    metadata,
    sa.Column("id", sa.BigInteger, comment="Provider凭证版本记录ID"),
    sa.Column("credential_id", sa.BigInteger, comment="Provider凭证ID"),
    sa.Column("version", sa.Integer, comment="Provider凭证版本"),
    sa.Column("api_base", sa.String(512), comment="Provider API地址"),
    sa.Column("api_key_ciphertext", sa.LargeBinary, comment="API Key密文"),
    sa.Column("api_key_nonce", sa.LargeBinary, comment="API Key加密随机数"),
    sa.Column("key_fingerprint", sa.String(32), comment="API Key指纹"),
    sa.Column("connection_test_status", sa.String(16), comment="连接测试状态"),
    sa.Column("connection_test_error", sa.String(500), comment="连接测试错误摘要"),
    sa.Column("tested_at", sa.DateTime(timezone=True), comment="连接测试时间"),
    sa.Column("created_by", sa.BigInteger, comment="创建用户ID"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    comment="Provider不可变凭证版本",
)

agent_conversation_binding_table = sa.Table(
    "ut_agent_conversation_binding",
    metadata,
    sa.Column("id", sa.BigInteger, comment="会话Agent绑定ID"),
    sa.Column("conversation_id", sa.BigInteger, comment="会话ID"),
    sa.Column("agent_id", sa.BigInteger, comment="Agent定义ID"),
    sa.Column("agent_version", sa.Integer, comment="当前Agent定义版本"),
    sa.Column("credential_id", sa.BigInteger, comment="当前Provider凭证ID"),
    sa.Column("credential_version", sa.Integer, comment="当前Provider凭证版本"),
    sa.Column("status", sa.String(32), comment="会话Agent绑定状态"),
    sa.Column("binding_version", sa.Integer, comment="会话绑定版本"),
    sa.Column("created_by", sa.BigInteger, comment="创建用户ID"),
    sa.Column("updated_by", sa.BigInteger, comment="更新用户ID"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="会话当前采用的Agent和Provider凭证版本",
)

agent_run_table = sa.Table(
    "ut_agent_run",
    metadata,
    sa.Column("id", sa.BigInteger, comment="Agent Run ID"),
    sa.Column("conversation_id", sa.BigInteger, comment="会话ID"),
    sa.Column("trigger_message_id", sa.BigInteger, comment="触发消息ID"),
    sa.Column("requester_user_id", sa.BigInteger, comment="请求用户ID"),
    sa.Column("binding_id", sa.BigInteger, comment="会话Agent绑定ID快照"),
    sa.Column("binding_version", sa.Integer, comment="会话绑定版本快照"),
    sa.Column("agent_id", sa.BigInteger, comment="Agent定义ID快照"),
    sa.Column("agent_version", sa.Integer, comment="Agent定义版本快照"),
    sa.Column("credential_id", sa.BigInteger, comment="Provider凭证ID快照"),
    sa.Column("credential_version", sa.Integer, comment="Provider凭证版本快照"),
    sa.Column("credential_owner_user_id", sa.BigInteger, comment="凭证所有者用户ID快照"),
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
    comment="Agent单次运行",
)

agent_run_step_table = sa.Table(
    "ut_agent_run_step",
    metadata,
    sa.Column("id", sa.BigInteger, comment="运行步骤ID"),
    sa.Column("run_id", sa.BigInteger, comment="Agent Run ID"),
    sa.Column("parent_step_id", sa.BigInteger, comment="父步骤ID"),
    sa.Column("sequence_no", sa.Integer, comment="步骤顺序"),
    sa.Column("step_type", sa.String(32), comment="步骤类型"),
    sa.Column("step_code", sa.String(64), comment="稳定步骤编码"),
    sa.Column("display_name", sa.String(128), comment="步骤显示名称"),
    sa.Column("status", sa.String(16), comment="步骤状态"),
    sa.Column("visibility", sa.String(16), comment="步骤可见范围"),
    sa.Column("input_summary_json", postgresql.JSONB, comment="脱敏输入摘要"),
    sa.Column("output_summary_json", postgresql.JSONB, comment="脱敏输出摘要"),
    sa.Column("error_code", sa.String(64), comment="步骤错误码"),
    sa.Column("error_message", sa.String(500), comment="步骤错误摘要"),
    sa.Column("started_at", sa.DateTime(timezone=True), comment="步骤开始时间"),
    sa.Column("finished_at", sa.DateTime(timezone=True), comment="步骤完成时间"),
    sa.Column("duration_ms", sa.Integer, comment="步骤耗时毫秒数"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="Agent结构化执行步骤",
)

agent_run_turn_table = sa.Table(
    "ut_agent_run_turn",
    metadata,
    sa.Column("id", sa.BigInteger, comment="Agent Turn ID"),
    sa.Column("run_id", sa.BigInteger, comment="Agent Run ID"),
    sa.Column("turn_no", sa.Integer, comment="Turn顺序号"),
    sa.Column("status", sa.String(16), comment="Turn状态"),
    sa.Column("stop_reason", sa.String(64), comment="Provider停止原因"),
    sa.Column("error_code", sa.String(64), comment="Turn错误码"),
    sa.Column("error_message", sa.String(500), comment="Turn错误摘要"),
    sa.Column("started_at", sa.DateTime(timezone=True), comment="Turn开始时间"),
    sa.Column("finished_at", sa.DateTime(timezone=True), comment="Turn完成时间"),
    sa.Column("duration_ms", sa.Integer, comment="Turn耗时毫秒数"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="Agent模型Turn",
)

agent_run_message_table = sa.Table(
    "ut_agent_run_message",
    metadata,
    sa.Column("id", sa.BigInteger, comment="Agent运行时消息ID"),
    sa.Column("run_id", sa.BigInteger, comment="Agent Run ID"),
    sa.Column("turn_id", sa.BigInteger, comment="Agent Turn ID"),
    sa.Column("turn_no", sa.Integer, comment="Turn顺序号"),
    sa.Column("message_key", sa.String(96), comment="运行时消息业务键"),
    sa.Column("role", sa.String(16), comment="消息角色"),
    sa.Column("status", sa.String(16), comment="运行时消息状态"),
    sa.Column("model_id", sa.String(128), comment="模型标识"),
    sa.Column("stop_reason", sa.String(64), comment="Provider停止原因"),
    sa.Column("input_tokens", sa.Integer, comment="输入Token数量"),
    sa.Column("output_tokens", sa.Integer, comment="输出Token数量"),
    sa.Column("error_code", sa.String(64), comment="消息错误码"),
    sa.Column("error_message", sa.String(500), comment="消息错误摘要"),
    sa.Column("started_at", sa.DateTime(timezone=True), comment="消息开始时间"),
    sa.Column("finished_at", sa.DateTime(timezone=True), comment="消息完成时间"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="Agent运行时消息",
)

agent_run_content_block_table = sa.Table(
    "ut_agent_run_content_block",
    metadata,
    sa.Column("id", sa.BigInteger, comment="内容块ID"),
    sa.Column("run_id", sa.BigInteger, comment="Agent Run ID"),
    sa.Column("turn_id", sa.BigInteger, comment="Agent Turn ID"),
    sa.Column("message_id", sa.BigInteger, comment="运行时消息ID"),
    sa.Column("content_index", sa.Integer, comment="内容块顺序号"),
    sa.Column("block_type", sa.String(16), comment="内容块类型"),
    sa.Column("status", sa.String(16), comment="内容块状态"),
    sa.Column("visibility", sa.String(16), comment="内容块可见范围"),
    sa.Column("content_text", sa.Text, comment="内容块完整正文"),
    sa.Column("tool_call_id", sa.String(96), comment="模型工具调用ID"),
    sa.Column("tool_name", sa.String(96), comment="模型工具名称"),
    sa.Column("started_at", sa.DateTime(timezone=True), comment="内容块开始时间"),
    sa.Column("finished_at", sa.DateTime(timezone=True), comment="内容块完成时间"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="Agent消息内容块",
)

agent_tool_call_table = sa.Table(
    "ut_agent_tool_call",
    metadata,
    sa.Column("id", sa.BigInteger, comment="工具调用ID"),
    sa.Column("run_id", sa.BigInteger, comment="Agent Run ID"),
    sa.Column("step_id", sa.BigInteger, comment="运行步骤ID"),
    sa.Column("turn_id", sa.BigInteger, comment="Agent Turn ID"),
    sa.Column("message_id", sa.BigInteger, comment="运行时消息ID"),
    sa.Column("tool_call_key", sa.String(96), comment="工具调用业务键"),
    sa.Column("tool_name", sa.String(96), comment="工具名称"),
    sa.Column("display_name", sa.String(128), comment="工具显示名称"),
    sa.Column("status", sa.String(16), comment="工具调用状态"),
    sa.Column("visibility", sa.String(16), comment="工具轨迹可见范围"),
    sa.Column("arguments_redacted_json", postgresql.JSONB, comment="脱敏调用参数"),
    sa.Column("result_summary_json", postgresql.JSONB, comment="脱敏结果摘要"),
    sa.Column("artifact_refs_json", postgresql.JSONB, comment="工具产物引用"),
    sa.Column("attempt", sa.SmallInteger, comment="调用尝试次数"),
    sa.Column("timeout_ms", sa.Integer, comment="调用超时毫秒数"),
    sa.Column("error_code", sa.String(64), comment="工具错误码"),
    sa.Column("error_message", sa.String(500), comment="工具错误摘要"),
    sa.Column("started_at", sa.DateTime(timezone=True), comment="调用开始时间"),
    sa.Column("finished_at", sa.DateTime(timezone=True), comment="调用完成时间"),
    sa.Column("duration_ms", sa.Integer, comment="调用耗时毫秒数"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="Agent工具调用轨迹",
)

agent_run_citation_table = sa.Table(
    "ut_agent_run_citation",
    metadata,
    sa.Column("id", sa.BigInteger, comment="回答引用ID"),
    sa.Column("run_id", sa.BigInteger, comment="Agent Run ID"),
    sa.Column("citation_key", sa.String(32), comment="回答引用标识"),
    sa.Column("source_type", sa.String(16), comment="引用来源类型"),
    sa.Column("message_id", sa.BigInteger, comment="来源消息ID"),
    sa.Column("asset_file_id", sa.BigInteger, comment="来源资产文件ID"),
    sa.Column("chunk_id", sa.BigInteger, comment="来源资源块ID"),
    sa.Column("resource_version", sa.Integer, comment="来源资源版本"),
    sa.Column("page_from", sa.Integer, comment="起始页码"),
    sa.Column("page_to", sa.Integer, comment="结束页码"),
    sa.Column("heading_path", sa.String(1024), comment="标题路径"),
    sa.Column("rank_no", sa.Integer, comment="引用排名"),
    sa.Column("retrieval_score", sa.Numeric(10, 6), comment="召回分数"),
    sa.Column("rerank_score", sa.Numeric(10, 6), comment="重排分数"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    comment="Agent回答引用",
)

agent_event_inbox_table = sa.Table(
    "ut_agent_event_inbox",
    metadata,
    sa.Column("event_id", sa.String(64), comment="事件ID"),
    sa.Column("event_type", sa.String(64), comment="事件类型"),
    sa.Column("aggregate_type", sa.String(32), comment="聚合类型"),
    sa.Column("aggregate_id", sa.String(64), comment="聚合ID"),
    sa.Column("status", sa.String(16), comment="消费状态"),
    sa.Column("attempt", sa.SmallInteger, comment="处理尝试次数"),
    sa.Column("last_error", sa.String(500), comment="最近错误摘要"),
    sa.Column("received_at", sa.DateTime(timezone=True), comment="首次接收时间"),
    sa.Column("processed_at", sa.DateTime(timezone=True), comment="处理完成时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="Agent事件消费幂等记录",
)

agent_message_segment_table = sa.Table(
    "ut_agent_message_segment",
    metadata,
    sa.Column("id", sa.BigInteger, comment="消息片段ID"),
    sa.Column("conversation_id", sa.BigInteger, comment="会话ID"),
    sa.Column("start_message_id", sa.BigInteger, comment="起始消息ID"),
    sa.Column("end_message_id", sa.BigInteger, comment="结束消息ID"),
    sa.Column("sender_id", sa.BigInteger, comment="消息发送者ID"),
    sa.Column("sender_display_name", sa.String(128), comment="发送者展示名称"),
    sa.Column("start_at", sa.DateTime(timezone=True), comment="片段开始时间"),
    sa.Column("end_at", sa.DateTime(timezone=True), comment="片段结束时间"),
    sa.Column("message_count", sa.Integer, comment="消息数量"),
    sa.Column("segment_text", sa.Text, comment="片段原文"),
    sa.Column("summary_text", sa.Text, comment="片段摘要"),
    sa.Column("summary_json", postgresql.JSONB, comment="结构化摘要"),
    sa.Column("token_count", sa.Integer, comment="Token数量"),
    sa.Column("milvus_pk", sa.String(96), comment="Milvus实体主键"),
    sa.Column("embedding_model_version", sa.String(128), comment="Embedding模型版本"),
    sa.Column("vector_status", sa.String(16), comment="向量状态"),
    sa.Column("status", sa.String(16), comment="消息片段状态"),
    sa.Column("source_revision", sa.Integer, comment="来源修订版本"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="长会话消息片段",
)

agent_message_segment_asset_table = sa.Table(
    "ut_agent_message_segment_asset",
    metadata,
    sa.Column("segment_id", sa.BigInteger, comment="消息片段ID"),
    sa.Column("conversation_id", sa.BigInteger, comment="会话ID"),
    sa.Column("message_id", sa.BigInteger, comment="消息ID"),
    sa.Column("asset_file_id", sa.BigInteger, comment="来源资产文件ID"),
    sa.Column("resource_version", sa.Integer, comment="来源资源版本"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    comment="消息片段资产血缘",
)

agent_resource_table = sa.Table(
    "ut_agent_resource",
    metadata,
    sa.Column("id", sa.BigInteger, comment="索引资源ID"),
    sa.Column("conversation_id", sa.BigInteger, comment="会话ID"),
    sa.Column("asset_file_id", sa.BigInteger, comment="资产文件ID"),
    sa.Column("resource_version", sa.Integer, comment="资源版本"),
    sa.Column("is_current", sa.Boolean, comment="是否为当前版本"),
    sa.Column("file_name", sa.String(512), comment="文件名称"),
    sa.Column("folder_id", sa.BigInteger, comment="目录ID"),
    sa.Column("path_text", sa.String(2048), comment="资源路径"),
    sa.Column("mime_type", sa.String(256), comment="MIME类型"),
    sa.Column("sha256", sa.String(64), comment="内容SHA256"),
    sa.Column("etag", sa.String(256), comment="来源ETag"),
    sa.Column("parser_version", sa.String(64), comment="解析器版本"),
    sa.Column("chunker_version", sa.String(64), comment="切块器版本"),
    sa.Column("embedding_model_version", sa.String(128), comment="Embedding模型版本"),
    sa.Column("status", sa.String(16), comment="资源索引状态"),
    sa.Column("error_code", sa.String(64), comment="资源错误码"),
    sa.Column("error_message", sa.String(500), comment="资源错误摘要"),
    sa.Column("chunk_count", sa.Integer, comment="资源块数量"),
    sa.Column("token_count", sa.Integer, comment="资源Token数量"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="Agent资源索引版本",
)

agent_asset_lifecycle_table = sa.Table(
    "ut_agent_asset_lifecycle",
    metadata,
    sa.Column("asset_file_id", sa.BigInteger, comment="资产文件ID"),
    sa.Column("conversation_id", sa.BigInteger, comment="会话ID"),
    sa.Column("resource_version", sa.Integer, comment="最新资源版本"),
    sa.Column("status", sa.String(16), comment="资产生命周期状态"),
    sa.Column("event_id", sa.String(64), comment="最新生命周期事件ID"),
    sa.Column("occurred_at", sa.DateTime(timezone=True), comment="最新事件发生时间"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="File Service资产生命周期投影",
)

agent_ingest_job_table = sa.Table(
    "ut_agent_ingest_job",
    metadata,
    sa.Column("id", sa.BigInteger, comment="入库任务ID"),
    sa.Column("asset_file_id", sa.BigInteger, comment="资产文件ID"),
    sa.Column("resource_version", sa.Integer, comment="资源版本"),
    sa.Column("stage", sa.String(32), comment="入库阶段"),
    sa.Column("status", sa.String(16), comment="入库任务状态"),
    sa.Column("attempt", sa.SmallInteger, comment="任务尝试次数"),
    sa.Column("worker_id", sa.String(128), comment="处理Worker标识"),
    sa.Column("error_code", sa.String(64), comment="任务错误码"),
    sa.Column("error_message", sa.String(500), comment="任务错误摘要"),
    sa.Column("started_at", sa.DateTime(timezone=True), comment="任务开始时间"),
    sa.Column("finished_at", sa.DateTime(timezone=True), comment="任务结束时间"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="Agent资源入库任务",
)

agent_resource_chunk_table = sa.Table(
    "ut_agent_resource_chunk",
    metadata,
    sa.Column("id", sa.BigInteger, comment="资源块ID"),
    sa.Column("resource_id", sa.BigInteger, comment="索引资源ID"),
    sa.Column("conversation_id", sa.BigInteger, comment="会话ID"),
    sa.Column("parent_chunk_id", sa.BigInteger, comment="父资源块ID"),
    sa.Column("chunk_type", sa.String(16), comment="资源块类型"),
    sa.Column("chunk_no", sa.Integer, comment="资源块顺序"),
    sa.Column("text_content", sa.Text, comment="资源块正文"),
    sa.Column("token_count", sa.Integer, comment="Token数量"),
    sa.Column("page_from", sa.Integer, comment="起始页码"),
    sa.Column("page_to", sa.Integer, comment="结束页码"),
    sa.Column("heading_path", sa.String(1024), comment="标题路径"),
    sa.Column("char_from", sa.Integer, comment="起始字符位置"),
    sa.Column("char_to", sa.Integer, comment="结束字符位置"),
    sa.Column("metadata_json", postgresql.JSONB, comment="资源块扩展元数据"),
    sa.Column("milvus_pk", sa.String(96), comment="Milvus实体主键"),
    sa.Column("vector_status", sa.String(16), comment="向量状态"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    comment="Agent资源切块",
)

agent_vector_collection_table = sa.Table(
    "ut_agent_vector_collection",
    metadata,
    sa.Column("id", sa.BigInteger, comment="向量Collection注册ID"),
    sa.Column("logical_name", sa.String(128), comment="逻辑名称"),
    sa.Column("physical_name", sa.String(255), comment="物理Collection名称"),
    sa.Column("alias_name", sa.String(255), comment="Milvus Alias名称"),
    sa.Column("cluster_key", sa.String(128), comment="Milvus集群标识"),
    sa.Column("schema_version", sa.Integer, comment="Collection Schema版本"),
    sa.Column("embedding_model", sa.String(128), comment="Embedding模型"),
    sa.Column("embedding_revision", sa.String(128), comment="Embedding模型修订"),
    sa.Column("vector_dimension", sa.Integer, comment="向量维度"),
    sa.Column("metric_type", sa.String(16), comment="向量距离类型"),
    sa.Column("index_type", sa.String(32), comment="向量索引类型"),
    sa.Column("index_params", postgresql.JSONB, comment="向量索引参数"),
    sa.Column("partition_count", sa.Integer, comment="Partition数量"),
    sa.Column("status", sa.String(20), comment="Collection状态"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    sa.Column("activated_at", sa.DateTime(timezone=True), comment="激活时间"),
    sa.Column("deprecated_at", sa.DateTime(timezone=True), comment="废弃时间"),
    sa.Column("updated_at", sa.DateTime(timezone=True), comment="更新时间"),
    comment="Agent向量Collection注册表",
)

agent_graph_checkpoint_table = sa.Table(
    "ut_agent_graph_checkpoint",
    metadata,
    sa.Column("id", sa.BigInteger, comment="Checkpoint记录ID"),
    sa.Column("thread_id", sa.String(64), comment="LangGraph Thread ID"),
    sa.Column("checkpoint_ns", sa.String(128), comment="Checkpoint命名空间"),
    sa.Column("checkpoint_id", sa.String(128), comment="Checkpoint ID"),
    sa.Column("parent_checkpoint_id", sa.String(128), comment="父Checkpoint ID"),
    sa.Column("checkpoint_type", sa.String(32), comment="Checkpoint序列化类型"),
    sa.Column("checkpoint_blob", sa.LargeBinary, comment="Checkpoint序列化内容"),
    sa.Column("metadata_type", sa.String(32), comment="Metadata序列化类型"),
    sa.Column("metadata_blob", sa.LargeBinary, comment="Checkpoint元数据"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    comment="LangGraph Checkpoint",
)

agent_graph_write_table = sa.Table(
    "ut_agent_graph_write",
    metadata,
    sa.Column("id", sa.BigInteger, comment="Checkpoint Write记录ID"),
    sa.Column("thread_id", sa.String(64), comment="LangGraph Thread ID"),
    sa.Column("checkpoint_ns", sa.String(128), comment="Checkpoint命名空间"),
    sa.Column("checkpoint_id", sa.String(128), comment="Checkpoint ID"),
    sa.Column("task_id", sa.String(128), comment="LangGraph Task ID"),
    sa.Column("task_path", sa.String(512), comment="LangGraph Task路径"),
    sa.Column("channel_name", sa.String(128), comment="Channel名称"),
    sa.Column("write_index", sa.Integer, comment="Write顺序"),
    sa.Column("value_type", sa.String(32), comment="Channel值序列化类型"),
    sa.Column("value_blob", sa.LargeBinary, comment="Channel序列化内容"),
    sa.Column("created_at", sa.DateTime(timezone=True), comment="创建时间"),
    comment="LangGraph中间Write",
)

# 只使用普通索引。业务唯一性由 advisory lock + 锁内查询保证。
sa.Index("idx_agent_definition_id", agent_definition_table.c.id)
sa.Index(
    "idx_agent_definition_owner",
    agent_definition_table.c.owner_type,
    agent_definition_table.c.owner_id,
    agent_definition_table.c.status,
)
sa.Index(
    "idx_agent_definition_version_agent",
    agent_definition_version_table.c.agent_id,
    agent_definition_version_table.c.version,
)
sa.Index("idx_agent_provider_credential_id", agent_provider_credential_table.c.id)
sa.Index(
    "idx_agent_provider_credential_owner",
    agent_provider_credential_table.c.owner_user_id,
    agent_provider_credential_table.c.status,
)
sa.Index(
    "idx_agent_provider_credential_scope",
    agent_provider_credential_table.c.usage_scope_type,
    agent_provider_credential_table.c.usage_scope_id,
    agent_provider_credential_table.c.status,
)
sa.Index(
    "idx_agent_provider_credential_version",
    agent_provider_credential_version_table.c.credential_id,
    agent_provider_credential_version_table.c.version,
)
sa.Index("idx_agent_conversation_binding_id", agent_conversation_binding_table.c.id)
sa.Index(
    "idx_agent_conversation_binding_conversation",
    agent_conversation_binding_table.c.conversation_id,
    agent_conversation_binding_table.c.status,
)
sa.Index(
    "idx_agent_conversation_binding_agent",
    agent_conversation_binding_table.c.agent_id,
    agent_conversation_binding_table.c.status,
)
sa.Index(
    "idx_agent_run_id",
    agent_run_table.c.id,
)
sa.Index(
    "idx_agent_run_trigger_message",
    agent_run_table.c.trigger_message_id,
)
sa.Index(
    "idx_agent_run_conversation_status",
    agent_run_table.c.conversation_id,
    agent_run_table.c.status,
    agent_run_table.c.created_at,
)
sa.Index(
    "idx_agent_run_conversation_queued_id",
    agent_run_table.c.conversation_id,
    agent_run_table.c.queued_at,
    agent_run_table.c.id,
)
sa.Index(
    "idx_agent_run_reconciliation",
    agent_run_table.c.status,
    agent_run_table.c.lease_expires_at,
)
sa.Index(
    "idx_agent_run_step_run_sequence",
    agent_run_step_table.c.run_id,
    agent_run_step_table.c.sequence_no,
)
sa.Index(
    "idx_agent_run_turn_run_no",
    agent_run_turn_table.c.run_id,
    agent_run_turn_table.c.turn_no,
)
sa.Index(
    "idx_agent_run_message_run_turn",
    agent_run_message_table.c.run_id,
    agent_run_message_table.c.turn_no,
)
sa.Index(
    "idx_agent_run_message_key",
    agent_run_message_table.c.message_key,
)
sa.Index(
    "idx_agent_run_content_block_message",
    agent_run_content_block_table.c.message_id,
    agent_run_content_block_table.c.content_index,
)
sa.Index(
    "idx_agent_tool_call_run",
    agent_tool_call_table.c.run_id,
    agent_tool_call_table.c.created_at,
)
sa.Index(
    "idx_agent_tool_call_key",
    agent_tool_call_table.c.tool_call_key,
)
sa.Index(
    "idx_agent_run_citation_run_rank",
    agent_run_citation_table.c.run_id,
    agent_run_citation_table.c.rank_no,
)
sa.Index(
    "idx_agent_event_inbox_event",
    agent_event_inbox_table.c.event_id,
)
sa.Index(
    "idx_agent_event_inbox_status",
    agent_event_inbox_table.c.status,
    agent_event_inbox_table.c.updated_at,
)
sa.Index(
    "idx_agent_message_segment_conversation_status",
    agent_message_segment_table.c.conversation_id,
    agent_message_segment_table.c.status,
)
sa.Index(
    "idx_agent_message_segment_message_range",
    agent_message_segment_table.c.start_message_id,
    agent_message_segment_table.c.end_message_id,
)
sa.Index(
    "idx_agent_message_segment_sender_time",
    agent_message_segment_table.c.conversation_id,
    agent_message_segment_table.c.sender_id,
    agent_message_segment_table.c.start_at,
)
sa.Index(
    "idx_agent_message_segment_asset_segment",
    agent_message_segment_asset_table.c.segment_id,
)
sa.Index(
    "idx_agent_message_segment_asset_asset",
    agent_message_segment_asset_table.c.asset_file_id,
    agent_message_segment_asset_table.c.conversation_id,
    agent_message_segment_asset_table.c.message_id,
)
sa.Index(
    "idx_agent_resource_asset_version",
    agent_resource_table.c.asset_file_id,
    agent_resource_table.c.resource_version,
)
sa.Index(
    "idx_agent_resource_conversation_current",
    agent_resource_table.c.conversation_id,
    agent_resource_table.c.is_current,
    agent_resource_table.c.status,
)
sa.Index(
    "idx_agent_asset_lifecycle_asset",
    agent_asset_lifecycle_table.c.asset_file_id,
)
sa.Index(
    "idx_agent_asset_lifecycle_conversation_status",
    agent_asset_lifecycle_table.c.conversation_id,
    agent_asset_lifecycle_table.c.status,
)
sa.Index(
    "idx_agent_ingest_job_asset_version",
    agent_ingest_job_table.c.asset_file_id,
    agent_ingest_job_table.c.resource_version,
)
sa.Index(
    "idx_agent_ingest_job_status",
    agent_ingest_job_table.c.status,
    agent_ingest_job_table.c.updated_at,
)
sa.Index(
    "idx_agent_resource_chunk_id",
    agent_resource_chunk_table.c.id,
)
sa.Index(
    "idx_agent_resource_chunk_resource_no",
    agent_resource_chunk_table.c.resource_id,
    agent_resource_chunk_table.c.chunk_no,
)
sa.Index(
    "idx_agent_resource_chunk_conversation_vector",
    agent_resource_chunk_table.c.conversation_id,
    agent_resource_chunk_table.c.vector_status,
)
sa.Index(
    "idx_agent_vector_collection_logical_status",
    agent_vector_collection_table.c.logical_name,
    agent_vector_collection_table.c.status,
)
sa.Index(
    "idx_agent_graph_checkpoint_thread",
    agent_graph_checkpoint_table.c.thread_id,
    agent_graph_checkpoint_table.c.checkpoint_ns,
    agent_graph_checkpoint_table.c.checkpoint_id,
)
sa.Index(
    "idx_agent_graph_write_checkpoint",
    agent_graph_write_table.c.thread_id,
    agent_graph_write_table.c.checkpoint_ns,
    agent_graph_write_table.c.checkpoint_id,
)
