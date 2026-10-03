package com.devin.uniontalk.common.storage.properties;

import com.devin.uniontalk.infrastructure.file.enums.StorageTypeEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 2026/6/30 14:26.
 *
 * <p>
 * Storage配置
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "union.storage")
public class StorageProperties {
    /**
     * 存储类型.
     */
    private StorageTypeEnum storageType;

    /**
     * 端点.
     */
    private String endpoint;

    /**
     * 暴露地址.
     */
    private String exportUrl;

    /**
     * 桶名.
     */
    private String bucketName;

    /**
     * AccessKey.
     */
    private String accessKey;

    /**
     * SecretKey.
     */
    private String secretKey;
}
