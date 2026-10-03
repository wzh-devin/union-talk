package com.devin.uniontalk.message.domain.vo.resp;

import com.devin.uniontalk.infrastructure.message.enums.MessageMentionTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/08/06 23:05.
 *
 * <p>
 * 消息提及响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "消息提及响应参数")
public class MessageMentionRespVO {

    /**
     * 提及类型.
     */
    @Schema(description = "提及类型")
    private MessageMentionTypeEnum mentionType;

    /**
     * 提及目标id.
     */
    @Schema(description = "提及目标id")
    private BigInteger targetId;

    /**
     * 正文展示文本.
     */
    @Schema(description = "正文展示文本")
    private String displayText;

    /**
     * 正文起始位置.
     */
    @Schema(description = "正文起始位置")
    private Integer startOffset;

    /**
     * 正文文本长度.
     */
    @Schema(description = "正文文本长度")
    private Integer length;
}
