package com.devin.uniontalk.infrastructure.message.enums;

/**
 * 2026/07/28 22:10.
 *
 * <p>
 * 消息发件箱状态枚举
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public enum MessageOutboxStatusEnum {

    /**
     * 等待发布.
     */
    PENDING,

    /**
     * 发布中.
     */
    PUBLISHING,

    /**
     * 已发布.
     */
    PUBLISHED,

    /**
     * 发布失败.
     */
    FAILED
}
