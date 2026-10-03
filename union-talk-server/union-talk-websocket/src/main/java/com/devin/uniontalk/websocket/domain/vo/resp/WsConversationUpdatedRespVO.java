package com.devin.uniontalk.websocket.domain.vo.resp;

import com.devin.uniontalk.infrastructure.message.enums.ConversationTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageSenderTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 2026/05/20 18:00.
 *
 * <p>
 * WebSocket 会话更新响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "WebSocket会话更新响应参数")
public class WsConversationUpdatedRespVO extends WsBaseRespVO<WsConversationUpdatedRespVO.WsConversationUpdatedRespData> {

    /**
     * WebSocket 会话更新响应数据.
     */
    @Data
    @Schema(description = "WebSocket会话更新响应数据")
    public static class WsConversationUpdatedRespData {

        /**
         * 会话id.
         */
        @Schema(description = "会话id")
        private BigInteger conversationId;

        /**
         * 会话类型.
         */
        @Schema(description = "会话类型")
        private ConversationTypeEnum type;

        /**
         * 群聊id.
         */
        @Schema(description = "群聊id")
        private BigInteger groupId;

        /**
         * 最后一条消息id.
         */
        @Schema(description = "最后一条消息id")
        private BigInteger lastMsgId;

        /**
         * 最后一条消息时间.
         */
        @Schema(description = "最后一条消息时间")
        private Date lastMsgAt;

        /**
         * 发送者id.
         */
        @Schema(description = "发送者id")
        private BigInteger senderId;

        /**
         * 消息发送者类型.
         */
        @Schema(description = "消息发送者类型")
        private MessageSenderTypeEnum senderType;

        /**
         * 更新时间.
         */
        @Schema(description = "更新时间")
        private Date updatedAt;
    }
}
