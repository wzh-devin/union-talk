package com.devin.uniontalk.message.domain.vo.resp;

import com.devin.uniontalk.infrastructure.file.enums.AssetFileTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/07/22 14:12.
 *
 * <p>
 * 消息资产文件信息响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "消息资产文件信息响应参数")
public class MessageAssetInfoRespVO {

    /**
     * 资产文件id.
     */
    @Schema(description = "资产文件id")
    private BigInteger id;

    /**
     * 会话id.
     */
    @Schema(description = "会话id")
    private BigInteger conversationId;

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
     * MIME类型.
     */
    @Schema(description = "MIME类型")
    private String mimeType;
}
