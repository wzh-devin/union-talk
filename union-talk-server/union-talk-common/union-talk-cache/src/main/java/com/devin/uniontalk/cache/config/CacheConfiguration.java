package com.devin.uniontalk.cache.config;

import com.devin.uniontalk.cache.properties.RedisProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.StringUtils;
import java.time.Duration;

/**
 * 2026/5/12 00:26.
 *
 * <p>
 * Redisson 配置类
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(RedisProperties.class)
public class CacheConfiguration {

    /**
     * 创建 RedisConnectionFactory.
     *
     * @param properties redis配置
     * @return RedisConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(final RedisProperties properties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.getHost());
        config.setPort(properties.getPort());
        config.setPassword(properties.getPassword());
        config.setDatabase(properties.getDatabase());

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(10))
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    /**
     * 创建 RedissonClient.
     *
     * @param properties redis配置
     * @return RedissonClient
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public RedissonClient redissonClient(final RedisProperties properties) {
        Config config = new Config();

        String protocol = properties.isSsl() ? "rediss://" : "redis://";
        String address = protocol + properties.getHost() + ":" + properties.getPort();

        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(properties.getDatabase())
                .setConnectionMinimumIdleSize(8)
                .setConnectionPoolSize(32)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        if (StringUtils.hasLength(properties.getPassword())) {
            singleServerConfig.setPassword(properties.getPassword());
        }

        return Redisson.create(config);
    }
}
