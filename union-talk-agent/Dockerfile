FROM ghcr.io/astral-sh/uv:python3.13-bookworm-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

RUN apt-get update \
    && apt-get install --yes --no-install-recommends \
        poppler-utils \
        tesseract-ocr \
        tesseract-ocr-chi-sim \
        tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

COPY pyproject.toml uv.lock README.md ./
RUN uv sync --locked --no-dev --no-install-project

COPY alembic.ini langgraph.json ./
COPY alembic ./alembic
COPY src ./src
RUN uv sync --locked --no-dev --no-editable

ENV PATH="/app/.venv/bin:${PATH}"

RUN groupadd --system agent \
    && useradd --system --gid agent --home-dir /app agent \
    && mkdir --parents /app/logs /app/nacos \
    && chown agent:agent /app/logs /app/nacos
USER agent

CMD ["agent-api"]
