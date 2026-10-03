package com.devin.uniontalk.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * WebSocket 长连接服务
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
public class UnionTalkWebSocketApplication {

    /**
     * 启动函数.
     *
     * @param args 参数
     */
    // CHECKSTYLE:OFF
    public static void main(final String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(UnionTalkWebSocketApplication.class, args);
        String port = context.getEnvironment().getProperty("server.port");
        String contextPath = context.getEnvironment().getProperty("server.servlet.context-path");
        StringBuilder swaggerPath = new StringBuilder()
                .append("http://localhost:")
                .append(port)
                .append(contextPath)
                .append("/doc.html");
        log.info("WebSocket ======> Swagger URL: {}", swaggerPath);
    }
    // CHECKSTYLE:ON
}
