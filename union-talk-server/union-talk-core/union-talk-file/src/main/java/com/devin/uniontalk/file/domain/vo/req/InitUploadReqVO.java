package com.devin.uniontalk.file.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import lombok.Data;

/**
 * 2026/06/30 18:40.
 *
 * <p>
 * 初始化上传任务请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "初始化上传任务请求参数")
public class InitUploadReqVO {

    /**
     * 会话id.
     */
    @NotNull(message = "会话id不能为空")
    @Schema(description = "会话id")
    private BigInteger conversationId;

    /**
     * 文件夹id.
     */
    @Schema(description = "文件夹id，不传时使用会话根目录")
    private BigInteger folderId;

    /**
     * 文件名称.
     */
    @NotBlank(message = "文件名称不能为空")
    @Size(max = 255, message = "文件名称长度超限")
    @Schema(description = "文件名称")
    private String fileName;

    /**
     * 文件大小.
     */
    @NotNull(message = "文件大小不能为空")
    @Schema(description = "文件大小")
    private BigInteger fileSize;

    /**
     * 文件SHA256.
     */
    @Size(max = 64, message = "文件SHA256长度超限")
    @Schema(description = "文件SHA256")
    private String fileSha256;

    /**
     * 分片大小.
     */
    @NotNull(message = "分片大小不能为空")
    @Schema(description = "分片大小")
    private BigInteger chunkSize;

    /**
     * MIME 类型.
     */
    @Size(max = 100, message = "MIME类型长度超限")
    @Schema(description = "MIME类型")
    private String mimeType;
}
