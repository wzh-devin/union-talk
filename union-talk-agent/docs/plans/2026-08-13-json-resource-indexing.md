# JSON 资源入库修复计划

> 日期：2026-08-13  
> 执行者：Codex

## 问题

`application/json` 上传事件已经经过 File Outbox、RabbitMQ 并到达 Index Worker，但
`DocumentParser` 未声明 JSON 解析策略，事件以 `RESOURCE_UNSUPPORTED` 进入终态失败。

## 文件范围

- `knowledge/application/document_parser.py`：JSON 类型识别与 JSONPath 文本提取。
- `knowledge/application/hierarchical_chunker.py`：控制超长单段 Parent 预算。
- `knowledge/application/resource_ingestion_service.py`：按处理组件版本决定幂等跳过或重建。
- `knowledge/domain/enums.py`：增加 JSON 文档领域枚举。
- `knowledge/domain/constants.py`：维护 Milvus 编码和 JSON 边界常量。
- `tests/test_document_indexing.py`：覆盖普通 JSON、内嵌 JSON、非法 JSON 和超长段落。

## 设计

JSON 使用现有 Parser 策略扩展，不增加旁路微服务或新数据表。对象字段、数组下标和标量
值转换为 `JSONPath: value`，字符串中合法的嵌套 JSON 递归展开。完成解析后复用统一的
Parent/Child Chunk、BGE-M3 和 Milvus 链路。

同一资产版本只有在 Parser、Chunker 和 Embedding 模型版本均一致时才复用 READY 结果；
处理组件升级后允许在业务锁内替换 PostgreSQL Chunk 和 Milvus 向量。

## 验收

- JSON 上传后资源进入 `READY`。
- PostgreSQL 同时保存 Parent 和 Child。
- Milvus 只写入 Child 向量。
- Parent 不超过 1600 估算 Token，Child 不超过 480 估算 Token。
- 非法 JSON 和超过层级、节点上限的 JSON 进入明确终态错误。
