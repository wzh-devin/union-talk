package com.devin.uniontalk.file.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.infrastructure.file.enums.UploadSessionStatusEnum;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import lombok.Data;

/**
 * 2026/06/30 18:20.
 *
 * <p>
 * 上传任务表(UploadSession)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_upload_session")
public class UploadSession implements Serializable {

    /**
     * 序列化版本号.
     */
    @Serial
    private static final long serialVersionUID = -23500989251982843L;

    /**
     * 失败原因最大长度.
     */
    private static final int FAILURE_REASON_MAX_LENGTH = 255;

    /**
     * 主键id.
     */
    @TableId
    private BigInteger id;

    /**
     * 上传token.
     */
    @TableField("upload_token")
    private String uploadToken;

    /**
     * 会话id.
     */
    @TableField("conversation_id")
    private BigInteger conversationId;

    /**
     * 文件夹id.
     */
    @TableField("folder_id")
    private BigInteger folderId;

    /**
     * 上传完成后的资产文件id.
     */
    @TableField("asset_id")
    private BigInteger assetId;

    /**
     * 文件名称.
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件扩展名.
     */
    @TableField("file_ext")
    private String fileExt;

    /**
     * mime类型.
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * 文件大小.
     */
    @TableField("file_size")
    private BigInteger fileSize;

    /**
     * 文件SHA256.
     */
    @TableField("file_sha256")
    private String fileSha256;

    /**
     * 分片大小.
     */
    @TableField("chunk_size")
    private BigInteger chunkSize;

    /**
     * 分片数量.
     */
    @TableField("chunk_count")
    private Integer chunkCount;

    /**
     * 已经上传的分片数量.
     */
    @TableField("uploaded_chunk_count")
    private Integer uploadedChunkCount;

    /**
     * 存储桶名称.
     */
    @TableField("bucket_name")
    private String bucketName;

    /**
     * 临时文件key前缀.
     */
    @TableField("temp_storage_key")
    private String tempStorageKey;

    /**
     * 上传状态.
     */
    @TableField("status")
    private String status;

    /**
     * 失败原因.
     */
    @TableField("failure_reason")
    private String failureReason;

    /**
     * 暂停时间.
     */
    @TableField("paused_at")
    private Date pausedAt;

    /**
     * 取消时间.
     */
    @TableField("canceled_at")
    private Date canceledAt;

    /**
     * 完成时间.
     */
    @TableField("completed_at")
    private Date completedAt;

    /**
     * 创建人.
     */
    @TableField("created_by")
    private BigInteger createdBy;

    /**
     * 创建时间.
     */
    @TableField("created_at")
    private Date createdAt;

    /**
     * 更新时间.
     */
    @TableField("updated_at")
    private Date updatedAt;

    /**
     * 初始化上传任务.
     *
     * @param id             任务id
     * @param uploadToken    上传token
     * @param conversationId 会话id
     * @param folderId       文件夹id
     * @param fileName       文件名称
     * @param fileExt        文件扩展名
     * @param mimeType       MIME 类型
     * @param fileSize       文件大小
     * @param fileSha256     文件摘要
     * @param chunkSize      分片大小
     * @param chunkCount     分片数量
     * @param bucketName     存储桶
     * @param userId         操作用户id
     */
    public void initSession(
            final BigInteger id,
            final String uploadToken,
            final BigInteger conversationId,
            final BigInteger folderId,
            final String fileName,
            final String fileExt,
            final String mimeType,
            final BigInteger fileSize,
            final String fileSha256,
            final BigInteger chunkSize,
            final Integer chunkCount,
            final String bucketName,
            final BigInteger userId
    ) {
        Date now = new Date();
        this.id = id;
        this.uploadToken = uploadToken;
        this.conversationId = conversationId;
        this.folderId = folderId;
        this.fileName = fileName;
        this.fileExt = fileExt;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.fileSha256 = fileSha256;
        this.chunkSize = chunkSize;
        this.chunkCount = chunkCount;
        this.uploadedChunkCount = 0;
        this.bucketName = bucketName;
        this.tempStorageKey = "upload/" + conversationId + "/" + id + "/";
        this.status = UploadSessionStatusEnum.UPLOADING.name();
        this.createdBy = userId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 校验可以上传分片.
     */
    public void validateCanUploadChunk() {
        AssertUtils.equal(UploadSessionStatusEnum.UPLOADING.name(), status, BizErrorEnum.UPLOAD_SESSION_STATUS_INVALID);
    }

    /**
     * 校验可以完成上传.
     */
    public void validateCanComplete() {
        AssertUtils.equal(UploadSessionStatusEnum.UPLOADING.name(), status, BizErrorEnum.UPLOAD_SESSION_STATUS_INVALID);
    }

    /**
     * 应用最终文件名称.
     *
     * @param finalFileName 最终文件名称
     */
    public void applyFinalFileName(final String finalFileName) {
        this.fileName = finalFileName;
        this.updatedAt = new Date();
    }

    /**
     * 暂停任务.
     */
    public void pause() {
        AssertUtils.equal(UploadSessionStatusEnum.UPLOADING.name(), status, BizErrorEnum.UPLOAD_SESSION_STATUS_INVALID);
        Date now = new Date();
        this.status = UploadSessionStatusEnum.PAUSED.name();
        this.pausedAt = now;
        this.updatedAt = now;
    }

    /**
     * 恢复任务.
     */
    public void resume() {
        AssertUtils.equal(UploadSessionStatusEnum.PAUSED.name(), status, BizErrorEnum.UPLOAD_SESSION_STATUS_INVALID);
        this.status = UploadSessionStatusEnum.UPLOADING.name();
        this.updatedAt = new Date();
    }

    /**
     * 取消任务.
     */
    public void cancel() {
        AssertUtils.isTrue(
                UploadSessionStatusEnum.UPLOADING.name().equals(status)
                        || UploadSessionStatusEnum.PAUSED.name().equals(status)
                        || UploadSessionStatusEnum.FAILED.name().equals(status),
                BizErrorEnum.UPLOAD_SESSION_STATUS_INVALID
        );
        Date now = new Date();
        this.status = UploadSessionStatusEnum.CANCELED.name();
        this.canceledAt = now;
        this.updatedAt = now;
    }

    /**
     * 标记任务完成.
     *
     * @param assetId 资产文件id
     */
    public void complete(final BigInteger assetId) {
        Date now = new Date();
        this.assetId = assetId;
        this.status = UploadSessionStatusEnum.COMPLETED.name();
        this.completedAt = now;
        this.updatedAt = now;
    }

    /**
     * 标记任务失败.
     *
     * @param failureReason 失败原因
     */
    public void fail(final String failureReason) {
        this.status = UploadSessionStatusEnum.FAILED.name();
        this.failureReason = getFailureReason(failureReason);
        this.updatedAt = new Date();
    }

    /**
     * 同步已上传分片数量.
     *
     * @param uploadedChunkCount 已上传分片数量
     */
    public void syncUploadedChunkCount(final Integer uploadedChunkCount) {
        this.uploadedChunkCount = uploadedChunkCount;
        this.updatedAt = new Date();
    }

    /**
     * 获取分片对象Key.
     *
     * @param chunkIndex 分片序号
     * @return 分片对象Key
     */
    public String getChunkStorageKey(final Integer chunkIndex) {
        return tempStorageKey + chunkIndex + ".part";
    }

    /**
     * 获取最终文件对象Key.
     *
     * @param assetId 资产文件id
     * @return 最终文件对象Key
     */
    public String getAssetStorageKey(final BigInteger assetId) {
        String keyPrefix = "asset/" + conversationId + "/member/" + createdBy + "/" + assetId;
        if (fileExt == null || fileExt.isEmpty()) {
            return keyPrefix;
        }
        return keyPrefix + "." + fileExt;
    }

    /**
     * 获取失败原因.
     *
     * @param failureReason 失败原因
     * @return 失败原因
     */
    private String getFailureReason(final String failureReason) {
        if (failureReason == null || failureReason.length() <= FAILURE_REASON_MAX_LENGTH) {
            return failureReason;
        }
        return failureReason.substring(0, FAILURE_REASON_MAX_LENGTH);
    }
}
