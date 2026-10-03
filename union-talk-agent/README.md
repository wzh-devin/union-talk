# Union Talk Agent

Union Talk 的 `@AI` 回答、单卡片流式状态、执行轨迹、DeepSeek 动态配置和
Milvus RAG 微服务。工程使用 Python 3.13、uv、FastAPI、LangGraph、PostgreSQL、
Redis、RabbitMQ 与 Milvus。

## 架构边界

```text
浏览器
  ├─ 普通聊天与最终 MESSAGE_CREATED ── WebSocket
  └─ Agent REST / 单 Run SSE ── Java Gateway ── Nacos ── agent-api

Message Outbox ── RabbitMQ ── answer-worker ── LangGraph
                                            ├─ Message gRPC：权限、紧凑上下文、回复写回
                                            ├─ Milvus：会话过滤的证据召回
                                            ├─ DeepSeek：流式生成
                                            └─ Redis：Snapshot、Stream、sequence
```

- 浏览器只访问 `/api/v1/agent/**`，不直连 Agent。
- Gateway 校验 Sa-Token，覆盖写入 `X-User-Id`。
- Agent 不信任浏览器角色，通过 Message gRPC 实时校验会话权限。
- 单个 Agent Run 使用 SSE；普通聊天和最终正式消息继续使用 WebSocket。
- Message Service 是聊天消息和 Agent 正式回复的唯一写入口。
- `agent-api` 注册 Nacos 临时实例；Worker 不注册 HTTP 服务。

## 当前实现状态

| 能力 | 状态 |
|---|---|
| `AGENT_MENTIONED` Inbox、Run 租约、失败重投与对账 | 已实现 |
| LangGraph、固定回答、DeepSeek 流式生成与首 Token 前重试 | 已实现 |
| Redis Snapshot/Stream、SSE 游标恢复、单卡片 sequence | 已实现 |
| DeepSeek 配置、API Key AES-GCM 加密和连接测试 | 已实现 |
| Message 权限、紧凑上下文与正式回复 gRPC 契约 | 已实现，需重建并重启 Java Message |
| Nacos 注册、注销和 Gateway `lb://union-talk-agent` 路由 | 已实现 |
| 共享 Milvus Collection、扫描 PDF OCR、Parent/Child 切块和召回核心 | 已实现 |
| File Service 资源 Outbox、事件、资源描述与临时下载契约 | 已实现，需执行 Java Flyway 并重启 File |
| BGE-M3 常驻 Embedding API、并发微批与多模式重排 | 已实现 |
| 生产压测、灰度和独立 Agent 公网鉴权 | 未完成 |

## 目录

```text
src/union_talk_agent/
├── access_control/     # 会话成员与管理权限端口
├── agent_config/       # 动态模型和会话 Agent 配置
├── agent_run/          # Run 聚合、应用用例、LangGraph、实时事件
├── event_inbox/        # RabbitMQ 至少一次投递幂等
├── knowledge/          # 文档解析、切块、资源索引和召回模型
├── infrastructure/     # PostgreSQL、Redis、RabbitMQ、gRPC、Nacos、模型、Milvus
├── interfaces/         # FastAPI 入站接口
├── settings/           # 环境与连接配置
└── bootstrap/          # API 和 Worker 进程入口
```

依赖方向固定为：

```text
Interfaces / Bootstrap → Application → Domain / Port ← Infrastructure
```

## 本地启动

```bash
cd /Users/devin/developer/coder/projects/union-talk/union-talk-agent
cp .env.example .env
uv sync --locked --all-groups
uv run alembic upgrade head
```

至少确认以下配置与 Java 服务一致：

```text
AGENT_GRPC_MESSAGE_TARGET=127.0.0.1:19005
AGENT_NACOS_SERVER_ADDRESSES=127.0.0.1:18848
AGENT_NACOS_NAMESPACE_ID=union-talk
AGENT_NACOS_GROUP_NAME=DEFAULT_GROUP
AGENT_NACOS_SERVICE_NAME=union-talk-agent
AGENT_NACOS_INSTANCE_IP=127.0.0.1
AGENT_REDIS_URL=redis://127.0.0.1:6379/0
AGENT_REDIS_PASSWORD=<与 Java UNION_REDIS_PASSWORD 一致>
```

RabbitMQ 的实际 vhost 是 `/union_talk`，AMQP URI 必须对开头的 `/` 编码：

```text
AGENT_RABBITMQ_URL=amqp://<user>:<password>@127.0.0.1:5672/%2Funion_talk
```

生成 API Key 主密钥并写入 `.env`：

```bash
uv run python -c 'import base64,secrets; print(base64.urlsafe_b64encode(secrets.token_bytes(32)).decode())'
```

启动 API：

```bash
uv run agent-api
```

启动回答和对账 Worker：

```bash
uv run answer-worker
uv run reconciliation-worker
```

启动本地 BGE-M3 与资源索引 Worker：

```bash
uv run embedding-api
uv run index-worker
```

`index-worker` 对普通 PDF 优先读取原生文本层，只对没有文本层的页面执行 OCR。
JSON 资源按 JSONPath 提取标量值；字符串内嵌的 JSON 会继续结构化展开，不会把花括号和
转义符原样作为检索正文。Parser、Chunker 或 Embedding 模型版本变化时，同一资产版本
允许幂等重建索引，避免旧 Inbox 成功状态阻断索引升级。
本机运行前需要安装 Poppler、Tesseract 和简体中文语言包；生产镜像已在
`Dockerfile` 中固化这些依赖：

```bash
brew install poppler tesseract
curl --fail --location \
  https://github.com/tesseract-ocr/tessdata_fast/raw/main/chi_sim.traineddata \
  --output "$(brew --prefix)/share/tessdata/chi_sim.traineddata"
tesseract --list-langs
```

OCR 配置使用 `AGENT_OCR_*`，默认语言为 `chi_sim+eng`、渲染分辨率为 220 DPI、
单页超时 60 秒、单文档最多 100 页。解析在线程池执行，不会阻塞 RabbitMQ 的
异步消费循环；临时故障延迟重试，超过 `AGENT_RABBITMQ_MAX_DELIVERY_ATTEMPTS`
后进入终态，避免热循环。

## 服务发现

`agent-api` 在 FastAPI lifespan 启动时注册 Nacos 临时实例，在进程关闭时注销：

```text
serviceName = union-talk-agent
group       = DEFAULT_GROUP
namespace   = union-talk
cluster     = DEFAULT
metadata    = protocol/http、version、healthPath
```

Gateway 的外部路径与 Agent 内部路径：

```text
/api/v1/agent/conversations/{id}/runs
        │ Gateway 去掉 /agent 服务段
        ▼
/api/v1/conversations/{id}/runs
```

Namespace、Group 和实例 IP 必须与 Gateway 的 Nacos Discovery 配置一致。实例 IP
必须能从 Gateway 所在网络访问，不能在跨容器部署时固定为容器内的
`127.0.0.1`。

## HTTP

浏览器使用 Gateway 外部路径：

```text
GET  /api/v1/agent/conversations/{conversationId}/config
PUT  /api/v1/agent/conversations/{conversationId}/config
POST /api/v1/agent/conversations/{conversationId}/config/test
GET  /api/v1/agent/conversations/{conversationId}/active-runs
GET  /api/v1/agent/conversations/{conversationId}/runs
GET  /api/v1/agent/runs/{runId}
GET  /api/v1/agent/runs/{runId}/snapshot
GET  /api/v1/agent/runs/{runId}/trace
GET  /api/v1/agent/runs/{runId}/events
POST /api/v1/agent/runs/{runId}/cancel
```

Nacos 或运维直接检查实例：

```text
GET /health/live
GET /health/ready
```

好友会话双方都可以管理当前会话共享的 Agent 配置；群聊只有群主和管理员可以管理。
会话成员可以查看当前会话的 Run、Snapshot、Trace 和 SSE；取消操作由发起者或群主、
管理员完成。

Agent API 只允许在可信内网被 Gateway 调用。若后续作为独立公网服务使用，必须先
增加 Token Introspection 或独立 OIDC 鉴权，不能让调用方直接构造
`X-User-Id`。

## 实时与单卡片

每个 Run 只有一张临时卡片：

```text
cardKey = agent-run:{runId}
```

Redis Key：

```text
ut:agent:run:{runId}:live
ut:agent:run:{runId}:events
ut:agent:run:{runId}:sequence
ut:agent:run:{runId}:cancel
```

前端使用带 `Authorization` 和 `Last-Event-ID` 的 `fetch` 读取 SSE。SSE 只更新
对应 Run 的卡片，用户同时发送和接收普通消息仍走 WebSocket。最终
`MESSAGE_CREATED.agentRunId` 到达后，正式消息替换临时卡片。

## Milvus

物理 Collection 固定为 `union_talk_rag_v2`，不会按会话创建 Collection：

- `conversation_id` 是 Partition Key 和强制过滤字段；
- `source_type`、`chunk_kind`、`mime_group` 在 Milvus 使用数字编码；
- PostgreSQL、Proto、HTTP 和事件继续使用可读字符串枚举；
- Milvus 只保存可重建向量与过滤元数据；
- 候选结果必须回 PostgreSQL 校验会话、当前版本和 `READY` 状态。

默认 Embedding 为 BGE-M3 1024 维，索引使用 HNSW，距离使用 COSINE。`embedding-api`
监听 `13008`，使用有界队列和短等待窗口合并并发编码请求；Answer/Index Worker 的
`AGENT_EMBEDDING_API_BASE` 必须指向 `http://127.0.0.1:13008`。

## 数据库约束

Alembic migration 位于 `alembic/versions/`。Agent 业务表遵守：

- 不使用 `PRIMARY KEY`、`UNIQUE`、`FOREIGN KEY`、`CHECK`、`NOT NULL`；
- 不使用 `REFERENCES` 或级联；
- 只建立普通索引；
- 表和字段包含简明 Comment；
- 必填、枚举、关联、幂等和状态流转由代码维护。

## 质量门禁

```bash
uv run ruff format --check src tests
uv run ruff check src tests
uv run pyright
uv run pytest
```

最后验证日期：2026-08-13。结果为 Ruff 零问题、Pyright 零错误零警告、
Pytest 64 passed；并验证 Pi Provider 事件、Thinking、工具循环、SSE V2、
异常中断恢复、指定资源不可用阻断、混合检索与 Embedding 微批队列。
