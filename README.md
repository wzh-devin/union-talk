# Union Talk

Union Talk is a full-stack communication workspace with a React frontend, Java services, and a Python agent service.

## Repository layout

```text
union-talk-frontend/  Vite + React + TypeScript client
union-talk-server/    Java backend services and shared modules
union-talk-agent/     FastAPI + LangGraph agent service
```

The frontend talks to the Java gateway. The gateway and backend services own user, conversation, message, file, and realtime communication data. The agent service handles agent runs, streaming state, retrieval, and model orchestration through the backend contracts.

## Development

See the README in each service for service-specific setup and commands:

- [`union-talk-frontend/README.md`](union-talk-frontend/README.md)
- [`union-talk-agent/README.md`](union-talk-agent/README.md)
- `union-talk-server/` contains the Maven multi-module backend.

Local `.env` files, dependency directories, IDE metadata, build output, and runtime logs are ignored by the root repository rules. Copy each service's `.env.example` before starting local development.
