package com.devin.uniontalk.infrastructure.message.enums;

/**
 * 2026/07/28 22:10.
 *
 * <p>
 * 消息发件箱事件类型枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public enum MessageOutboxEventTypeEnum {

    /**
     * 消息创建事件.
     */
    MESSAGE_CREATED,

    /**
     * 会话更新事件.
     */
    CONVERSATION_UPDATED,

    /**
     * Agent提及事件.
     */
    AGENT_MENTIONED
}
