package com.devin.uniontalk.infrastructure.message.constant;

/**
 * 2026/07/28 23:45.
 *
 * <p>
 * 消息Agent常量
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class MessageAgentConstant {

    /**
     * Agent回复幂等键前缀.
     */
    public static final String REPLY_IDEMPOTENCY_KEY_PREFIX = "agent-reply:";

    /**
     * 模型标识最大长度.
     */
    public static final int MODEL_ID_MAX_LENGTH = 128;

    /**
     * 单条Agent回复最大引用数量.
     */
    public static final int CITATION_MAX_COUNT = 100;

    /**
     * 引用标识最大长度.
     */
    public static final int CITATION_KEY_MAX_LENGTH = 32;

    /**
     * 引用标题路径最大长度.
     */
    public static final int CITATION_HEADING_PATH_MAX_LENGTH = 1024;

    /**
     * Agent默认显示名称.
     */
    public static final String DEFAULT_DISPLAY_NAME = "AI助手";

    /**
     * 用户信息不可用时的显示名称.
     */
    public static final String DEFAULT_USER_DISPLAY_NAME = "会话成员";

    /**
     * 系统消息显示名称.
     */
    public static final String SYSTEM_DISPLAY_NAME = "系统";

    /**
     * Agent上下文最近消息最小数量.
     */
    public static final int CONTEXT_RECENT_MESSAGE_MIN_COUNT = 1;

    /**
     * Agent上下文最近消息最大数量.
     */
    public static final int CONTEXT_RECENT_MESSAGE_MAX_COUNT = 200;

    /**
     * 私有构造方法，避免实例化.
     */
    private MessageAgentConstant() {
    }
}
