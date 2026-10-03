package com.devin.uniontalk.base.config;

import com.devin.uniontalk.base.properties.CryptoProperties;
import com.devin.uniontalk.base.utils.CryptoUtils;
import com.devin.uniontalk.base.utils.SpringContextHolder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 2026/5/11 22:16.
 *
 * <p>
 * 基础配置
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(CryptoProperties.class)
public class BaseConfiguration {

    /**
     * SpringContextHolder.
     *
     * @return SpringContextHolder
     */
    @Bean
    public SpringContextHolder springContextHolder() {
        return new SpringContextHolder();
    }

    /**
     * CryptoUtils.
     *
     * @param properties CryptoProperties
     * @return CryptoUtils
     */
    @Bean
    public CryptoUtils cryptoUtils(final CryptoProperties properties) {
        CryptoUtils cryptoUtils = new CryptoUtils();
        cryptoUtils.init(properties.getSecretKey());
        return cryptoUtils;
    }

}
