package com.devin.uniontalk.file.domain.vo.resp;

import com.devin.uniontalk.infrastructure.file.enums.AssetFileStatusEnum;
import com.devin.uniontalk.infrastructure.file.enums.AssetFileTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.util.Date;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/06/30 18:40.
 *
 * <p>
 * 资产文件响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "资产文件响应参数")
public class AssetFileRespVO {

    /**
     * 文件id.
     */
    @Schema(description = "文件id")
    private BigInteger id;

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
     * 文件名称.
     */
    @Schema(description = "文件名称")
    private String name;

    /**
     * 文件扩展名.
     */
    @Schema(description = "文件扩展名")
    private String fileExt;

    /**
     * 文件类型.
     */
    @Schema(description = "文件类型")
    private AssetFileTypeEnum fileType;

    /**
     * 文件大小.
     */
    @Schema(description = "文件大小")
    private BigInteger fileSize;

    /**
     * MIME 类型.
     */
    @Schema(description = "MIME类型")
    private String mimeType;

    /**
     * 文件状态.
     */
    @Schema(description = "文件状态")
    private AssetFileStatusEnum status;

    /**
     * 创建时间.
     */
    @Schema(description = "创建时间")
    private Date createdAt;

    /**
     * 当前用户文件权限.
     */
    @Schema(description = "当前用户文件权限")
    private AssetFilePermissionRespVO permissions;
}
