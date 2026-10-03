package com.devin.uniontalk.message.domain.vo.resp;

import com.devin.uniontalk.infrastructure.message.enums.AgentCitationSourceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/07/28 22:12.
 *
 * <p>
 * Agent引用响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "Agent引用响应参数")
public class AgentCitationRespVO {

    /**
     * 引用标识.
     */
    @Schema(description = "引用标识")
    private String citationKey;

    /**
     * 来源类型.
     */
    @Schema(description = "来源类型")
    private AgentCitationSourceTypeEnum sourceType;

    /**
     * 消息id.
     */
    @Schema(description = "消息id")
    private BigInteger messageId;

    /**
     * 资产文件id.
     */
    @Schema(description = "资产文件id")
    private BigInteger assetFileId;

    /**
     * 资源版本.
     */
    @Schema(description = "资源版本")
    private Integer resourceVersion;

    /**
     * 资源块id.
     */
    @Schema(description = "资源块id")
    private BigInteger chunkId;

    /**
     * 起始页码.
     */
    @Schema(description = "起始页码")
    private Integer pageFrom;

    /**
     * 结束页码.
     */
    @Schema(description = "结束页码")
    private Integer pageTo;

    /**
     * 标题路径.
     */
    @Schema(description = "标题路径")
    private String headingPath;
}
