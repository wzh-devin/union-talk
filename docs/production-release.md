# Union Talk 生产发布方案

本仓库使用单仓库、多服务、多镜像的发布模型。生产发布只由版本 Tag 触发，`main` 分支推送不会直接构建或部署生产环境。每个选中的镜像都构建为 `linux/amd64` 与 `linux/arm64` 多架构 manifest，并同时推送版本 Tag、`latest` 和 `sha-<commit>`。

## 方案结论

每个可独立运行的服务构建一个镜像，不把前端、Java 服务和 Agent 打进一个大镜像。这样可以让一次小改动只重新构建和重启目标服务，也能为每个服务保留独立的回滚版本。

当前镜像与 Compose 文件的对应关系如下：

| 服务 | 镜像 | Compose 文件 | 选择服务的 Git Tag |
| --- | --- | --- | --- |
| 前端 | `union-talk-frontend` | `union-talk-frontend/docker-compose.production.yaml` | `frontend-v1.2.3` |
| Gateway | `union-talk-gateway` | `union-talk-server/deploy/docker/docker-compose.gateway.yaml` | `gateway-v1.2.3` |
| Auth | `union-talk-auth` | `union-talk-server/deploy/docker/docker-compose.auth.yaml` | `auth-v1.2.3` |
| User | `union-talk-user` | `union-talk-server/deploy/docker/docker-compose.user.yaml` | `user-v1.2.3` |
| Message | `union-talk-message` | `union-talk-server/deploy/docker/docker-compose.message.yaml` | `message-v1.2.3` |
| File | `union-talk-file` | `union-talk-server/deploy/docker/docker-compose.file.yaml` | `file-v1.2.3` |
| WebSocket | `union-talk-websocket` | `union-talk-server/deploy/docker/docker-compose.websocket.yaml` | `websocket-v1.2.3` |
| Agent 整体 | `union-talk-agent` | `union-talk-agent/docker-compose.production.yaml` | `agent-v1.2.3` |

镜像推送到 GHCR，命名为：

```text
ghcr.io/wzh-devin/union-talk/<image>:v1.2.3
```

每个镜像还会带一个 `sha-<commit>` 标签和 `latest` 标签。生产 Compose 使用不带服务前缀的语义版本 Tag，不能使用 `latest`；`latest` 是给人工拉取和其他环境使用的可变别名。服务前缀只用于选择发布范围：例如 Git Tag `gateway-v1.2.4` 只发布 Gateway，但镜像使用 `:v1.2.4`，不会把 `gateway-` 写入镜像 Tag。服务 Tag 只更新目标服务的 `latest`，不会改写其他服务。

Buildx 使用 QEMU 构建两个平台。工作流会在部署前执行 `docker buildx imagetools inspect`，只要版本 Tag 或 `latest` 缺少 `linux/amd64`、`linux/arm64` 任一平台，就不会进入生产部署。

Agent 镜像包含 PyTorch、BGE-M3 和 CUDA 相关 Python 依赖；本地 ARM64 QEMU 构建已通过，但耗时约 22 分钟。若 GitHub runner 的构建窗口或磁盘不足，再把 Agent 单独迁移到原生 ARM runner，其他服务无需改变。

## Tag 规则

完整发布使用普通版本 Tag：

```bash
git tag v1.2.3
git push origin v1.2.3
```

这会构建并部署全部镜像。只发布一个服务时使用服务前缀 Tag：

```bash
git tag gateway-v1.2.4
git push origin gateway-v1.2.4
```

这只会构建 `union-talk-gateway:v1.2.4`，并只更新 Gateway 的 Compose 项目。其他服务继续使用服务器上原来的版本。Agent 的 `agent-v1.0.1` 同理只发布 Agent，镜像为 `union-talk-agent:v1.0.1`。

User、Message、File、Gateway 之间有接口和服务发现关系。涉及跨服务契约的修改使用完整 `vX.Y.Z` 发布；只有保持兼容的单服务修改才使用服务前缀 Tag。版本 Tag 创建后不复用，回滚使用之前保留的镜像版本。

## Action 执行流程

`.github/workflows/release-production.yml` 的执行顺序是：

1. 解析 Tag，得到版本号和需要发布的服务集合。
2. 如果集合包含 Java 服务，使用 Java 21 和 Maven 一次性打包 Java 多模块工程；当前生产发布暂时使用 `-DskipTests`。
3. 使用 Docker Buildx + QEMU 为选中的服务构建 `linux/amd64,linux/arm64` 独立镜像，并推送版本、`latest` 和 SHA 标签到 GHCR。Java 镜像复用同一份 Maven 构建产物，前端和 Agent 使用各自的 Dockerfile。
4. 检查所有目标镜像的两个平台 manifest。
5. 通过 SSH 把选中的生产 Compose 文件同步到部署目录。
6. 在服务器上执行 `docker compose pull` 和 `docker compose up -d --force-recreate --remove-orphans --no-build`，只重启选中的 Compose 项目并清理已删除的旧服务容器。
7. 输出 Compose 状态；部署目录中的 `.env`、`agent.env` 和数据目录始终留在服务器，不进入 GitHub 仓库。

主分支不触发生产 Action。后续如果需要主分支质量检查，可以增加独立的 CI workflow，但不要把它和生产部署绑定。

## 服务器一次性准备

服务器需要准备 Docker Engine、Docker Compose V2，以及与应用容器共享的外部网络：

```bash
mkdir -p /opt/union-talk
cd /opt/union-talk
docker network create union-talk 2>/dev/null || true
```

在 `/opt/union-talk/.env` 放置 Java 服务的部署配置，在 `/opt/union-talk/agent.env` 放置 Agent 的 `AGENT_*` 配置。两个文件只存在于服务器，不提交到仓库。

可以从仓库模板开始准备：

```bash
cp union-talk-server/deploy/docker/.env.example /opt/union-talk/.env
cp union-talk-agent/.env.example /opt/union-talk/agent.env
chmod 600 /opt/union-talk/.env /opt/union-talk/agent.env
```

然后替换其中的示例地址、账号、密钥和模型配置。`.env` 中的 `UNION_TALK_IMAGE_TAG` 由 Action 通过环境变量覆盖，服务器保留的值只作为手工回滚默认值。

Agent 使用独立的单容器多进程 Compose 项目，生产环境至少应把 Embedding 地址设置为同一容器的回环地址：

```dotenv
AGENT_GRPC_MESSAGE_TARGET=union-talk-message:19005
AGENT_GRPC_FILE_TARGET=union-talk-file:19006
AGENT_EMBEDDING_API_BASE=http://127.0.0.1:13008
```

Java 服务的 Nacos、PostgreSQL、Redis、RabbitMQ、Seata、MinIO 和 SMTP 配置继续放在 `.env` 与 Nacos 中，Compose 不会覆盖这些运行时配置。

## GitHub Environment 配置

在仓库的 `production` Environment 中配置以下 Secrets：

| Secret | 用途 |
| --- | --- |
| `SSH_HOST` | 生产服务器地址 |
| `SSH_PORT` | SSH 端口，不填时默认 `22` |
| `SSH_USER` | 部署用户 |
| `SSH_PRIVATE_KEY` | 部署用户的 SSH 私钥 |
| `SSH_KNOWN_HOSTS` | `ssh-keyscan` 得到的服务器主机指纹 |
| `DEPLOY_DIR` | 部署目录，例如 `/opt/union-talk` |
| `GHCR_READ_USERNAME` | 服务器拉取 GHCR 的账号 |
| `GHCR_READ_TOKEN` | 只授予 GHCR 拉取权限的 Token |

部署用户需要能够执行 Docker 命令。GHCR 包可以设置为私有；Action 使用内置 `GITHUB_TOKEN` 推送，服务器使用单独的只读 Token 拉取。不要把服务器 `.env` 或 GHCR Token 写入仓库 Secret 以外的日志、命令参数或 Compose 文件。

## 回滚

镜像使用不可变版本 Tag，回滚只需要把目标 Compose 使用的 Tag 改回上一个已验证版本，然后在服务器执行：

```bash
cd /opt/union-talk
export UNION_TALK_IMAGE_PREFIX=ghcr.io/wzh-devin/union-talk
export UNION_TALK_IMAGE_TAG=v1.2.2
docker compose --env-file .env -f docker-compose.gateway.yaml pull
docker compose --env-file .env -f docker-compose.gateway.yaml up -d --force-recreate
```

前端和 Agent 分别设置 `UNION_TALK_FRONTEND_TAG`、`UNION_TALK_AGENT_TAG`。不要把 `latest` 用于回滚；数据库迁移和 Nacos 配置如果与镜像版本绑定，需要随版本一起备份并按同一版本回退。
