package com.devin.uniontalk.file.domain.vo.resp;

import com.devin.uniontalk.infrastructure.file.enums.UploadSessionStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/06/30 18:40.
 *
 * <p>
 * 上传任务响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "上传任务响应参数")
public class UploadSessionRespVO {

    /**
     * 上传任务id.
     */
    @Schema(description = "上传任务id")
    private BigInteger sessionId;

    /**
     * 上传token.
     */
    @Schema(description = "上传token")
    private String uploadToken;

    /**
     * 会话id.
     */
    @Schema(description = "会话id")
    private BigInteger conversationId;

    /**
     * 文件夹id.
     */
    @Schema(description = "文件夹id")
    private BigInteger folderId;

    /**
     * 资产文件id.
     */
    @Schema(description = "资产文件id")
    private BigInteger assetId;

    /**
     * 文件名称.
     */
    @Schema(description = "文件名称")
    private String fileName;

    /**
     * 文件扩展名.
     */
    @Schema(description = "文件扩展名")
    private String fileExt;

    /**
     * MIME 类型.
     */
    @Schema(description = "MIME类型")
    private String mimeType;

    /**
     * 文件大小.
     */
    @Schema(description = "文件大小")
    private BigInteger fileSize;

    /**
     * 分片大小.
     */
    @Schema(description = "分片大小")
    private BigInteger chunkSize;

    /**
     * 分片数量.
     */
    @Schema(description = "分片数量")
    private Integer chunkCount;

    /**
     * 已上传分片数量.
     */
    @Schema(description = "已上传分片数量")
    private Integer uploadedChunkCount;

    /**
     * 已上传分片序号列表.
     */
    @Schema(description = "已上传分片序号列表")
    private List<Integer> chunkIndexList;

    /**
     * 上传状态.
     */
    @Schema(description = "上传状态")
    private UploadSessionStatusEnum status;

    /**
     * 创建时间.
     */
    @Schema(description = "创建时间")
    private Date createdAt;

    /**
     * 更新时间.
     */
    @Schema(description = "更新时间")
    private Date updatedAt;
}
