package com.devin.uniontalk.common.storage.service;

import com.devin.uniontalk.common.storage.domain.model.StorageObjectWriteResult;
import java.io.InputStream;
import java.util.List;

/**
 * 2026/06/30 18:10.
 *
 * <p>
 * 对象存储服务
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ObjectStorageService {

    /**
     * 上传对象.
     *
     * @param bucketName  存储桶名称
     * @param storageKey  对象Key
     * @param inputStream 对象输入流
     * @param objectSize  对象大小
     * @param contentType 内容类型
     * @return 对象写入结果
     */
    StorageObjectWriteResult putObject(
            String bucketName,
            String storageKey,
            InputStream inputStream,
            long objectSize,
            String contentType
    );

    /**
     * 合并对象.
     *
     * @param bucketName       存储桶名称
     * @param storageKey       合并后的对象Key
     * @param sourceKeyList    源对象Key列表
     * @param contentType      内容类型
     * @return 对象写入结果
     */
    StorageObjectWriteResult composeObject(
            String bucketName,
            String storageKey,
            List<String> sourceKeyList,
            String contentType
    );

    /**
     * 生成对象预签名读取地址.
     *
     * @param bucketName                存储桶名称
     * @param storageKey                对象Key
     * @param responseContentType       浏览器响应内容类型
     * @param responseContentDisposition 浏览器响应内容处置方式
     * @param expirySeconds             有效期，单位为秒
     * @return 预签名读取地址
     */
    String getPresignedObjectUrl(
            String bucketName,
            String storageKey,
            String responseContentType,
            String responseContentDisposition,
            int expirySeconds
    );

    /**
     * 删除对象.
     *
     * @param bucketName 存储桶名称
     * @param storageKey 对象Key
     */
    void removeObject(String bucketName, String storageKey);

    /**
     * 批量删除对象.
     *
     * @param bucketName     存储桶名称
     * @param storageKeyList 对象Key列表
     */
    void removeObjectList(String bucketName, List<String> storageKeyList);
}
