package com.devin.uniontalk.infrastructure.message.enums;

/**
 * 2026/07/28 23:48.
 *
 * <p>
 * 消息发件箱聚合类型枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public enum MessageOutboxAggregateTypeEnum {

    /**
     * 消息聚合.
     */
    MESSAGE,

    /**
     * 会话聚合.
     */
    CONVERSATION
}
