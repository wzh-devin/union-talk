package com.devin.uniontalk.base.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 2026/5/13 14:36.
 *
 * <p>
 * 加解密配置属性
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "union.crypto")
public class CryptoProperties {

    /**
     * 密钥.
     */
    private String secretKey;
}
