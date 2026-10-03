package com.devin.uniontalk.message.domain.model;

import com.devin.uniontalk.infrastructure.message.enums.MessageSenderTypeEnum;
import java.math.BigInteger;
import java.util.Date;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/07/31 13:20.
 *
 * <p>
 * Agent紧凑会话消息
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
public class AgentConversationMessage {

    /**
     * 消息id.
     */
    private BigInteger messageId;

    /**
     * 发送者类型.
     */
    private MessageSenderTypeEnum senderType;

    /**
     * 发送者显示名称.
     */
    private String senderDisplayName;

    /**
     * 文本内容.
     */
    private String content;

    /**
     * 创建时间.
     */
    private Date createdAt;
}
