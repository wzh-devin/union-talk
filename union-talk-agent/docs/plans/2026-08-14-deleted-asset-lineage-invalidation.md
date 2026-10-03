# 删除资产的派生知识失效计划

## 文件清单

- `interfaces/mq/schemas/message_index_event.py`：读取消息事件中的 Agent 引用快照。
- `knowledge/domain/message_segment.py`：保存消息片段引用的资产 ID 列表。
- `infrastructure/postgres/tables/agent_tables.py`：声明消息片段与资产血缘关系表。
- `alembic/versions/20260814_0007_message_asset_lineage.py`：创建无外键、无唯一约束的血缘表和普通索引。
- `infrastructure/postgres/dao/message_segment_dao.py`：写入血缘、按资产失效派生消息索引。
- `knowledge/application/resource_deletion_service.py`：编排资源和派生消息索引失效。
- `knowledge/application/ports/vector_index_writer.py`：增加批量消息向量删除契约。
- `infrastructure/milvus/resource_vector_index.py`：按会话和消息 ID 批量删除派生向量。
- `infrastructure/postgres/dao/knowledge_evidence_dao.py`：召回回表时排除已删除资源的派生消息。
- `agent_run/graph/answer_graph.py`：近期消息进入 Prompt 前排除已失效派生消息。
- `tests/`：覆盖事件血缘、关系失效、向量清理和召回过滤。

## 执行流程

1. Message Service 的 `MESSAGE_CREATED` 事件继续携带 `citationList`，Python 将其中 `RESOURCE_CHUNK.assetFileId` 去重后写入消息索引命令。
   `MESSAGE_SEGMENT` 引用必须使用正式聊天消息 ID，不能使用 Agent 内部 Segment ID。
2. 消息片段写入时同步替换 `ut_agent_message_segment_asset` 血缘记录。
3. `ASSET_DELETED` 消费事务内先写生命周期 `DELETED`，再失效资源、Chunk 和引用该资产的消息片段。
4. 事务提交后删除资产向量，并按消息 ID 批量删除派生消息向量。
5. 召回回表通过资源生命周期二次校验；近期消息进入 Prompt 前排除已失效消息 ID。
6. 聊天消息本身继续展示，不删除 Message Service 的历史记录。

标准 Migration 和运行时代码均不直接访问 Message Service 业务表。当前本地开发数据已通过一次性数据修复建立血缘；新数据完全依赖 `MESSAGE_CREATED.citationList` 事件写入。

## 数据契约

`ut_agent_message_segment_asset` 字段：`segment_id`、`conversation_id`、`message_id`、`asset_file_id`、`resource_version`、`created_at`。仅建立普通索引，不建立主键、唯一键、外键或 Check；关系行随消息片段整体替换，不单独暴露聚合 ID。

## 验收

- 删除文件后资源检索不能返回其 Chunk。
- 删除文件后消息检索不能返回引用该文件的 AI 回复。
- 近期消息上下文不能再次注入该 AI 回复。
- 聊天界面仍保留历史 AI 回复以供审计。
- 一条回复引用多个文件时，删除任意一个来源文件即使该回复退出知识召回。
