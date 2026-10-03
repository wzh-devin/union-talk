FROM registry.cn-hangzhou.aliyuncs.com/wzh-pull/eclipse-temurin:21-jre-alpine

ARG UNION_TALK_VERSION=1.0.0-SNAPSHOT

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS=""

WORKDIR /app

COPY target/union-talk-message-${UNION_TALK_VERSION}.jar /app/app.jar

EXPOSE 13005 19005

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
