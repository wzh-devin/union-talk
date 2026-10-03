package com.devin.uniontalk.common.storage.service.impl;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.exception.BizException;
import com.devin.uniontalk.common.storage.domain.model.StorageObjectWriteResult;
import com.devin.uniontalk.common.storage.service.ObjectStorageService;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * 2026/06/30 18:10.
 *
 * <p>
 * MinIO对象存储服务
 * </p>
 *
 * @param minioClient       MinIO 内部访问客户端
 * @param exportMinioClient MinIO 公开地址签名客户端
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public record MinioObjectStorageServiceImpl(
        MinioClient minioClient,
        MinioClient exportMinioClient
) implements ObjectStorageService {

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
    @Override
    public StorageObjectWriteResult putObject(
            final String bucketName,
            final String storageKey,
            final InputStream inputStream,
            final long objectSize,
            final String contentType
    ) {
        try {
            ObjectWriteResponse response = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey)
                            .stream(inputStream, objectSize, -1)
                            .contentType(contentType)
                            .build()
            );
            return toWriteResult(bucketName, storageKey, response.etag());
        } catch (Exception e) {
            log.error("上传对象失败, bucketName={}, storageKey={}", bucketName, storageKey, e);
            throw new BizException(BizErrorEnum.STORAGE_OBJECT_FAILED);
        }
    }

    /**
     * 合并对象.
     *
     * @param bucketName    存储桶名称
     * @param storageKey    合并后的对象Key
     * @param sourceKeyList 源对象Key列表
     * @param contentType   内容类型
     * @return 对象写入结果
     */
    @Override
    public StorageObjectWriteResult composeObject(
            final String bucketName,
            final String storageKey,
            final List<String> sourceKeyList,
            final String contentType
    ) {
        try {
            List<ComposeSource> sourceList = sourceKeyList.stream()
                    .map(sourceKey -> ComposeSource.builder()
                            .bucket(bucketName)
                            .object(sourceKey)
                            .build())
                    .toList();
            ComposeObjectArgs.Builder builder = ComposeObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storageKey)
                    .sources(sourceList);
            if (Objects.nonNull(contentType)) {
                builder.headers(Map.of("Content-Type", contentType));
            }
            ObjectWriteResponse response = minioClient.composeObject(builder.build());
            return toWriteResult(bucketName, storageKey, response.etag());
        } catch (Exception e) {
            log.error("合并对象失败, bucketName={}, storageKey={}, sourceKeyList={}", bucketName, storageKey, sourceKeyList, e);
            throw new BizException(BizErrorEnum.STORAGE_OBJECT_FAILED);
        }
    }

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
    @Override
    public String getPresignedObjectUrl(
            final String bucketName,
            final String storageKey,
            final String responseContentType,
            final String responseContentDisposition,
            final int expirySeconds
    ) {
        try {
            Map<String, String> responseQueryMap = Map.of(
                    "response-content-disposition", responseContentDisposition,
                    "response-content-type", responseContentType
            );
            return exportMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(storageKey)
                            .extraQueryParams(responseQueryMap)
                            .expiry(expirySeconds)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成对象预签名地址失败, bucketName={}, storageKey={}", bucketName, storageKey, e);
            throw new BizException(BizErrorEnum.STORAGE_OBJECT_FAILED);
        }
    }

    /**
     * 删除对象.
     *
     * @param bucketName 存储桶名称
     * @param storageKey 对象Key
     */
    @Override
    public void removeObject(final String bucketName, final String storageKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storageKey)
                            .build()
            );
        } catch (Exception e) {
            log.warn("删除对象失败, bucketName={}, storageKey={}", bucketName, storageKey, e);
        }
    }

    /**
     * 批量删除对象.
     *
     * @param bucketName     存储桶名称
     * @param storageKeyList 对象Key列表
     */
    @Override
    public void removeObjectList(final String bucketName, final List<String> storageKeyList) {
        if (Objects.isNull(storageKeyList) || storageKeyList.isEmpty()) {
            return;
        }
        try {
            Iterable<Result<DeleteError>> deleteResults = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(storageKeyList.stream().map(DeleteObject::new).toList())
                            .build()
            );
            deleteResults.forEach(deleteResult -> logDeleteError(bucketName, deleteResult));
        } catch (Exception e) {
            log.warn("批量删除对象失败, bucketName={}, storageKeyList={}", bucketName, storageKeyList, e);
        }
    }

    /**
     * 记录对象删除失败信息.
     *
     * @param bucketName   存储桶名称
     * @param deleteResult 删除结果
     */
    private void logDeleteError(final String bucketName, final Result<DeleteError> deleteResult) {
        try {
            DeleteError deleteError = deleteResult.get();
            log.warn("批量删除对象失败, bucketName={}, storageKey={}, message={}",
                    bucketName,
                    deleteError.objectName(),
                    deleteError.message()
            );
        } catch (Exception e) {
            log.warn("读取批量删除对象结果失败, bucketName={}", bucketName, e);
        }
    }

    /**
     * 转换对象写入结果.
     *
     * @param bucketName 存储桶名称
     * @param storageKey 对象Key
     * @param etag       对象ETag
     * @return 对象写入结果
     */
    private StorageObjectWriteResult toWriteResult(
            final String bucketName,
            final String storageKey,
            final String etag
    ) {
        return StorageObjectWriteResult.builder()
                .bucketName(bucketName)
                .storageKey(storageKey)
                .etag(etag)
                .build();
    }
}
