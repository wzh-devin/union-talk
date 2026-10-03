package com.devin.uniontalk.message.domain.vo.req;

import com.devin.uniontalk.infrastructure.message.constant.MessageMentionConstant;
import com.devin.uniontalk.infrastructure.message.enums.MessageMentionTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import lombok.Data;

/**
 * 2026/08/06 23:05.
 *
 * <p>
 * 消息提及请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "消息提及请求参数")
public class MessageMentionReqVO {

    /**
     * 提及类型.
     */
    @NotNull(message = "提及类型不能为空")
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
    @NotBlank(message = "提及展示文本不能为空")
    @Size(max = MessageMentionConstant.DISPLAY_TEXT_MAX_LENGTH, message = "提及展示文本长度超限")
    @Schema(description = "正文展示文本")
    private String displayText;

    /**
     * 正文起始位置.
     */
    @NotNull(message = "提及起始位置不能为空")
    @Min(value = 0, message = "提及起始位置非法")
    @Schema(description = "正文起始位置")
    private Integer startOffset;

    /**
     * 正文文本长度.
     */
    @NotNull(message = "提及文本长度不能为空")
    @Min(value = 1, message = "提及文本长度非法")
    @Schema(description = "正文文本长度")
    private Integer length;

}
