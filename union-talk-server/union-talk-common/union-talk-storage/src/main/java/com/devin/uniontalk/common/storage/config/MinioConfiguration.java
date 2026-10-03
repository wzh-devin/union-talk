package com.devin.uniontalk.common.storage.config;

import com.devin.uniontalk.common.storage.properties.StorageProperties;
import com.devin.uniontalk.common.storage.service.ObjectStorageService;
import com.devin.uniontalk.common.storage.service.impl.MinioObjectStorageServiceImpl;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 2026/6/30 14:17.
 *
 * <p>
 * Minio配置
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties(StorageProperties.class)
public class MinioConfiguration {

    /**
     * 存储配置.
     */
    private final StorageProperties properties;

    /**
     * 创建 MinioClient 对象.
     *
     * @return MinioClient
     */
    @Bean
    @ConditionalOnProperty(prefix = "union.storage", name = "storage-type", havingValue = "MINIO")
    public MinioClient minioClient() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
        initBucket(minioClient);
        return minioClient;
    }

    /**
     * 创建 MinioAsyncClient 对象.
     *
     * @return MinioAsyncClient
     */
    @Bean
    @ConditionalOnProperty(prefix = "union.storage", name = "storage-type", havingValue = "MINIO")
    public MinioAsyncClient minioAsyncClient() {
        return MinioAsyncClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    /**
     * 创建对象存储服务.
     *
     * @param minioClient MinioClient
     * @return 对象存储服务
     */
    @Bean
    @ConditionalOnProperty(prefix = "union.storage", name = "storage-type", havingValue = "MINIO")
    public ObjectStorageService objectStorageService(final MinioClient minioClient) {
        MinioClient exportMinioClient = MinioClient.builder()
                .endpoint(properties.getExportUrl())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
        return new MinioObjectStorageServiceImpl(minioClient, exportMinioClient);
    }

    /**
     * 初始化存储桶.
     *
     * @param minioClient MinioClient
     */
    private void initBucket(final MinioClient minioClient) {
        log.info("======> 初始化 [{}] 存储桶", properties.getBucketName());
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(properties.getBucketName())
                            .build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(properties.getBucketName())
                                .build()
                );
            }
            log.info("<====== [{}] 存储桶初始化完成", properties.getBucketName());
        } catch (Exception e) {
            log.error("{}存储桶初始化失败: {}", properties.getBucketName(), e.getMessage());
            throw new RuntimeException("InitBucket Error: " + e.getMessage());
        }
    }
}
