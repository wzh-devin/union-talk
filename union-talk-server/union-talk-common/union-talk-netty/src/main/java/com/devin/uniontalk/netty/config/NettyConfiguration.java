package com.devin.uniontalk.netty.config;

import com.devin.uniontalk.netty.properties.NettyWebSocketProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * Netty 自动配置入口
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(NettyWebSocketProperties.class)
public class NettyConfiguration {
}
