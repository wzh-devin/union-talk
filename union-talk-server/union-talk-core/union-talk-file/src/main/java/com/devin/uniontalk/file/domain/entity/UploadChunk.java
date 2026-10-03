package com.devin.uniontalk.file.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devin.uniontalk.infrastructure.file.enums.UploadChunkStatusEnum;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import lombok.Data;

/**
 * 2026/06/30 18:20.
 *
 * <p>
 * 上传分片表(UploadChunk)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_upload_chunk")
public class UploadChunk implements Serializable {

    /**
     * 序列化版本号.
     */
    @Serial
    private static final long serialVersionUID = -81409919097408756L;

    /**
     * 主键id.
     */
    @TableId
    private BigInteger id;

    /**
     * 任务id.
     */
    @TableField("session_id")
    private BigInteger sessionId;

    /**
     * 分片序号.
     */
    @TableField("chunk_index")
    private Integer chunkIndex;

    /**
     * 分片大小.
     */
    @TableField("chunk_size")
    private BigInteger chunkSize;

    /**
     * 分片SHA256.
     */
    @TableField("chunk_sha256")
    private String chunkSha256;

    /**
     * 临时分片对象Key.
     */
    @TableField("storage_key")
    private String storageKey;

    /**
     * 对象存储ETag.
     */
    @TableField("etag")
    private String etag;

    /**
     * 状态.
     */
    @TableField("status")
    private String status;

    /**
     * 上传时间.
     */
    @TableField("uploaded_at")
    private Date uploadedAt;

    /**
     * 初始化分片记录.
     *
     * @param id          分片记录id
     * @param sessionId   上传任务id
     * @param chunkIndex  分片序号
     * @param chunkSize   分片大小
     * @param chunkSha256 分片SHA256
     * @param storageKey  分片对象Key
     * @param etag        对象存储ETag
     */
    public void initChunk(
            final BigInteger id,
            final BigInteger sessionId,
            final Integer chunkIndex,
            final BigInteger chunkSize,
            final String chunkSha256,
            final String storageKey,
            final String etag
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.chunkIndex = chunkIndex;
        this.chunkSize = chunkSize;
        this.chunkSha256 = chunkSha256;
        this.storageKey = storageKey;
        this.etag = etag;
        this.status = UploadChunkStatusEnum.SUCCESS.name();
        this.uploadedAt = new Date();
    }

    /**
     * 判断是否相同分片.
     *
     * @param chunkSha256 分片SHA256
     * @return true表示相同
     */
    public boolean isSameChunk(final String chunkSha256) {
        return this.chunkSha256.equalsIgnoreCase(chunkSha256);
    }
}
