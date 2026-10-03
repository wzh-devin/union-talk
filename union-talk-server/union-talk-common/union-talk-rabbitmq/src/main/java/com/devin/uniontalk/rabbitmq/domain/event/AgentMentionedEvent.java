package com.devin.uniontalk.rabbitmq.domain.event;

import com.devin.uniontalk.infrastructure.message.enums.MessageOutboxEventTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import java.math.BigInteger;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/07/28 22:18.
 *
 * <p>
 * Agent提及事件
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
public class AgentMentionedEvent {

    /**
     * 事件id.
     */
    private String eventId;

    /**
     * 事件类型.
     */
    private MessageOutboxEventTypeEnum eventType;

    /**
     * 事件版本.
     */
    private Integer schemaVersion;

    /**
     * 触发消息id.
     */
    private BigInteger triggerMessageId;

    /**
     * 会话id.
     */
    private BigInteger conversationId;

    /**
     * 被提及的稳定Agent定义id.
     */
    private BigInteger agentId;

    /**
     * 请求用户id.
     */
    private BigInteger requesterUserId;

    /**
     * 请求用户显示名称.
     */
    private String requesterDisplayName;

    /**
     * 消息类型.
     */
    private MessageTypeEnum messageType;

    /**
     * 事件发生时间.
     */
    private Date occurredAt;
}
