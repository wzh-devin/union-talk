package com.devin.uniontalk.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 2026/5/11 01:10.
 *
 * <p>
 * 认证服务
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
public class UnionTalkAuthApplication {

    /**
     * 启动函数.
     *
     * @param args 参数
     */
    // CHECKSTYLE:OFF
    public static void main(final String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(UnionTalkAuthApplication.class, args);
        String port = context.getEnvironment().getProperty("server.port");
        String contextPath = context.getEnvironment().getProperty("server.servlet.context-path");
        StringBuilder swaggerPath = new StringBuilder()
                .append("http://localhost:")
                .append(port)
                .append(contextPath)
                .append("/doc.html");
        log.info("Auth ======> Swagger URL: {}", swaggerPath);
    }
    // CHECKSTYLE:ON
}
