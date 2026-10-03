package com.devin.uniontalk.rabbitmq.domain.event;

import com.devin.uniontalk.infrastructure.message.enums.ConversationTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageSenderTypeEnum;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/05/20 18:00.
 *
 * <p>
 * 会话更新事件
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationUpdatedEvent {

    /**
     * 事件id.
     */
    private String eventId;

    /**
     * 会话id.
     */
    private BigInteger conversationId;

    /**
     * 会话类型.
     */
    private ConversationTypeEnum type;

    /**
     * 群聊id.
     */
    private BigInteger groupId;

    /**
     * 最后一条消息id.
     */
    private BigInteger lastMsgId;

    /**
     * 最后一条消息时间.
     */
    private Date lastMsgAt;

    /**
     * 发送者id.
     */
    private BigInteger senderId;

    /**
     * 消息发送者类型.
     */
    private MessageSenderTypeEnum senderType;

    /**
     * 需要推送的用户id列表.
     */
    private List<BigInteger> receiverUserIdList;

    /**
     * 更新时间.
     */
    private Date updatedAt;
}
