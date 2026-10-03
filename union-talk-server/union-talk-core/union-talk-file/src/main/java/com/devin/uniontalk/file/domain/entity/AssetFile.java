package com.devin.uniontalk.file.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.infrastructure.file.enums.AssetFilePreviewMimeTypeEnum;
import com.devin.uniontalk.infrastructure.file.enums.AssetFileStatusEnum;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.Locale;
import lombok.Data;

/**
 * 2026/06/30 18:20.
 *
 * <p>
 * 资产文件(AssetFile)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_asset_file")
public class AssetFile implements Serializable {

    /**
     * 序列化版本号.
     */
    @Serial
    private static final long serialVersionUID = -75971426728219409L;

    /**
     * 主键id.
     */
    @TableId
    private BigInteger id;

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
     * 文件名称.
     */
    @TableField("name")
    private String name;

    /**
     * 文件扩展名.
     */
    @TableField("file_ext")
    private String fileExt;

    /**
     * 文件类型.
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 文件大小.
     */
    @TableField("file_size")
    private BigInteger fileSize;

    /**
     * 存储类型.
     */
    @TableField("storage_type")
    private String storageType;

    /**
     * 存储Key.
     */
    @TableField("storage_key")
    private String storageKey;

    /**
     * 存储桶.
     */
    @TableField("bucket_name")
    private String bucketName;

    /**
     * mime类型.
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * SHA256.
     */
    @TableField("sha256")
    private String sha256;

    /**
     * 文件状态.
     */
    @TableField("status")
    private String status;

    /**
     * 元数据JSON.
     */
    @TableField("metadata_json")
    private String metadataJson;

    /**
     * 对象存储ETag.
     */
    @TableField("etag")
    private String etag;

    /**
     * 内容版本.
     */
    @TableField("resource_version")
    private Integer resourceVersion;

    /**
     * 删除时间.
     */
    @TableField("deleted_at")
    private Date deletedAt;

    /**
     * 创建人.
     */
    @TableField("created_by")
    private BigInteger createdBy;

    /**
     * 更新人.
     */
    @TableField("updated_by")
    private BigInteger updatedBy;

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
     * 按上传任务初始化文件实体.
     *
     * @param id          文件id
     * @param session     上传任务
     * @param fileType    文件类型
     * @param storageType 存储类型
     * @param storageKey  存储Key
     * @param etag        对象ETag
     * @param userId      操作用户id
     */
    public void initByUploadSession(
            final BigInteger id,
            final UploadSession session,
            final String fileType,
            final String storageType,
            final String storageKey,
            final String etag,
            final BigInteger userId
    ) {
        Date now = new Date();
        this.id = id;
        this.conversationId = session.getConversationId();
        this.folderId = session.getFolderId();
        this.name = session.getFileName();
        this.fileExt = session.getFileExt();
        this.fileType = fileType;
        this.fileSize = session.getFileSize();
        this.storageType = storageType;
        this.storageKey = storageKey;
        this.bucketName = session.getBucketName();
        this.mimeType = session.getMimeType();
        this.sha256 = session.getFileSha256();
        this.status = AssetFileStatusEnum.NORMAL.name();
        this.metadataJson = "{}";
        this.etag = etag;
        this.resourceVersion = 1;
        this.createdBy = userId;
        this.updatedBy = userId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 获取适合浏览器预览的 MIME 类型.
     *
     * @return 文件预览 MIME 类型
     */
    public String getPreviewMimeType() {
        return AssetFilePreviewMimeTypeEnum.getMimeType(fileExt, mimeType);
    }

    /**
     * 移动文件到指定目录.
     *
     * @param targetFolderId 目标目录id
     * @param targetName     目标文件名称
     * @param userId         操作用户id
     */
    public void moveTo(
            final BigInteger targetFolderId,
            final String targetName,
            final BigInteger userId
    ) {
        this.folderId = targetFolderId;
        this.name = targetName;
        this.updatedBy = userId;
        this.updatedAt = new Date();
    }

    /**
     * 重命名文件.
     *
     * @param newName 新文件名
     * @param userId  操作用户id
     */
    public void rename(final String newName, final BigInteger userId) {
        AssertUtils.isTrue(isSameFileExt(newName), BizErrorEnum.ASSET_FILE_RENAME_EXT_CHANGED);
        this.name = newName;
        this.updatedBy = userId;
        this.updatedAt = new Date();
    }

    /**
     * 标记文件已删除.
     *
     * @param userId 操作用户id
     */
    public void markDeleted(final BigInteger userId) {
        Date now = new Date();
        this.status = AssetFileStatusEnum.DELETED.name();
        this.deletedAt = now;
        this.updatedBy = userId;
        this.updatedAt = now;
    }

    /**
     * 判断文件扩展名是否保持一致.
     *
     * @param newName 新文件名
     * @return true表示扩展名一致
     */
    private boolean isSameFileExt(final String newName) {
        String newFileExt = getFileExt(newName);
        return fileExt.equalsIgnoreCase(newFileExt);
    }

    /**
     * 获取文件扩展名.
     *
     * @param fileName 文件名
     * @return 文件扩展名
     */
    private String getFileExt(final String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
