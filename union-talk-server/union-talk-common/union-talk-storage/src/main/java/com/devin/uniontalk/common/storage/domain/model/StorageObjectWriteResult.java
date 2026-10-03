package com.devin.uniontalk.common.storage.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * 2026/06/30 18:10.
 *
 * <p>
 * 对象写入结果
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
public class StorageObjectWriteResult {

    /**
     * 存储桶名称.
     */
    private String bucketName;

    /**
     * 对象存储Key.
     */
    private String storageKey;

    /**
     * 对象 ETag.
     */
    private String etag;
}
