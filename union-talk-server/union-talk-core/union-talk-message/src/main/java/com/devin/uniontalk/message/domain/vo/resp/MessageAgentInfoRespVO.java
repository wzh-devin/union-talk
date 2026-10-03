package com.devin.uniontalk.message.domain.vo.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/07/28 22:12.
 *
 * <p>
 * 消息Agent信息响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "消息Agent信息响应参数")
public class MessageAgentInfoRespVO {

    /**
     * 稳定Agent定义id.
     */
    @Schema(description = "稳定Agent定义id")
    private BigInteger agentId;

    /**
     * Agent显示名称.
     */
    @Schema(description = "Agent显示名称")
    private String displayName;
}
