package com.devin.uniontalk.message.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigInteger;
import lombok.Data;

/**
 * 2026/05/20 16:00.
 *
 * <p>
 * 会话已读请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "会话已读请求参数")
public class ReadConversationReqVO {

    /**
     * 会话id.
     */
    @NotNull(message = "会话id不能为空")
    @Schema(description = "会话id")
    private BigInteger conversationId;

    /**
     * 最后已读消息id.
     */
    @Schema(description = "最后已读消息id")
    private BigInteger lastReadMsgId;
}
