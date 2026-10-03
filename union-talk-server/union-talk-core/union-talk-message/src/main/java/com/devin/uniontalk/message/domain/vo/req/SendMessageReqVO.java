package com.devin.uniontalk.message.domain.vo.req;

import com.devin.uniontalk.infrastructure.message.constant.MessageMentionConstant;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.util.List;
import lombok.Data;

/**
 * 2026/05/20 16:00.
 *
 * <p>
 * 发送消息请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "发送消息请求参数")
public class SendMessageReqVO {

    /**
     * 会话id.
     */
    @NotNull(message = "会话id不能为空")
    @Schema(description = "会话id")
    private BigInteger conversationId;

    /**
     * 消息类型.
     */
    @NotNull(message = "消息类型不能为空")
    @Schema(description = "消息类型")
    private MessageTypeEnum type;

    /**
     * 消息内容.
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容长度超限")
    @Schema(description = "消息内容")
    private String content;

    /**
     * 引用消息id.
     */
    @Schema(description = "引用消息id")
    private BigInteger quoteMsgId;

    /**
     * 结构化提及列表.
     */
    @Size(max = MessageMentionConstant.MAX_COUNT, message = "消息提及数量超限")
    @Schema(description = "结构化提及列表")
    private List<@Valid MessageMentionReqVO> mentionList;
}
