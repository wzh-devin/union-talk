# syntax=docker/dockerfile:1.7

FROM node:22.22.0-alpine AS build

ENV PNPM_HOME=/pnpm \
    PATH=/pnpm:$PATH \
    HUSKY=0

WORKDIR /app

RUN corepack enable \
    && corepack prepare pnpm@11.1.2 --activate \
    && pnpm config set store-dir /pnpm/store

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./

RUN --mount=type=cache,id=union-talk-frontend-pnpm,target=/pnpm/store \
    pnpm install --frozen-lockfile

COPY . .

RUN pnpm build

FROM nginx:1.28-alpine AS runtime

ENV GATEWAY_UPSTREAM=http://host.docker.internal:13000 \
    WEBSOCKET_UPSTREAM=http://host.docker.internal:14001

RUN rm -f /etc/nginx/conf.d/default.conf

COPY docker/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1/healthz || exit 1
