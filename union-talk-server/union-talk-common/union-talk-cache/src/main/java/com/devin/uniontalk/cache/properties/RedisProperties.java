package com.devin.uniontalk.cache.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 2026/5/12 00:30.
 *
 * <p>
 * Redis 配置属性
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "union.redis")
public class RedisProperties {
    /**
     * Redis 服务器地址.
     */
    private String host = "127.0.0.1";

    /**
     * Redis 端口.
     */
    private int port = 6379;

    /**
     * Redis 密码.
     */
    private String password;

    /**
     * Redis 数据库索引（默认为 0 索引）.
     */
    private int database = 0;

    /**
     * Redis SSL.
     */
    private boolean ssl = false;
}
