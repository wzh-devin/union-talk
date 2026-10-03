package com.devin.uniontalk.rabbitmq.constant;

/**
 * 2026/05/20 00:00.
 *
 * <p>
 * RabbitMQ 常量
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class RabbitMqConstant {

    /**
     * Agent提及事件Schema版本.
     */
    public static final int AGENT_MENTIONED_EVENT_SCHEMA_VERSION = 3;

    /**
     * 消息创建事件Schema版本.
     */
    public static final int MESSAGE_CREATED_EVENT_SCHEMA_VERSION = 2;

    /**
     * 资产内容事件Schema版本.
     */
    public static final int ASSET_CONTENT_CHANGED_SCHEMA_VERSION = 1;

    /**
     * 资产删除事件Schema版本.
     */
    public static final int ASSET_DELETED_SCHEMA_VERSION = 1;

    /**
     * 文件事件交换机.
     */
    public static final String FILE_EXCHANGE = "union.talk.file.exchange";

    /**
     * 资产内容变更路由键.
     */
    public static final String ASSET_CONTENT_CHANGED_ROUTING_KEY = "union.talk.file.asset.content.changed";

    /**
     * 资产删除路由键.
     */
    public static final String ASSET_DELETED_ROUTING_KEY = "union.talk.file.asset.deleted";

    /**
     * 好友申请交换机.
     */
    public static final String FRIEND_REQUEST_EXCHANGE = "union.talk.friend.request.exchange";

    /**
     * 好友申请 WebSocket 推送队列.
     */
    public static final String FRIEND_REQUEST_WEBSOCKET_QUEUE = "union.talk.friend.request.websocket.queue";

    /**
     * 好友申请路由键.
     */
    public static final String FRIEND_REQUEST_ROUTING_KEY = "union.talk.friend.request.created";

    /**
     * 好友申请同意交换机.
     */
    public static final String FRIEND_REQUEST_ACCEPTED_EXCHANGE = "union.talk.friend.request.accepted.exchange";

    /**
     * 好友申请同意 WebSocket 推送队列.
     */
    public static final String FRIEND_REQUEST_ACCEPTED_WEBSOCKET_QUEUE = "union.talk.friend.request.accepted.websocket.queue";

    /**
     * 好友申请同意路由键.
     */
    public static final String FRIEND_REQUEST_ACCEPTED_ROUTING_KEY = "union.talk.friend.request.accepted";

    /**
     * 消息事件交换机.
     */
    public static final String MESSAGE_EXCHANGE = "union.talk.message.exchange";

    /**
     * 消息创建 WebSocket 推送队列.
     */
    public static final String MESSAGE_CREATED_WEBSOCKET_QUEUE = "union.talk.message.created.websocket.queue";

    /**
     * 消息创建路由键.
     */
    public static final String MESSAGE_CREATED_ROUTING_KEY = "union.talk.message.created";

    /**
     * Agent提及队列.
     */
    public static final String AGENT_MENTIONED_QUEUE = "union.talk.message.agent.mentioned.queue";

    /**
     * Agent提及路由键.
     */
    public static final String AGENT_MENTIONED_ROUTING_KEY = "union.talk.message.agent.mentioned";

    /**
     * 会话事件交换机.
     */
    public static final String CONVERSATION_EXCHANGE = "union.talk.conversation.exchange";

    /**
     * 会话更新 WebSocket 推送队列.
     */
    public static final String CONVERSATION_UPDATED_WEBSOCKET_QUEUE = "union.talk.conversation.updated.websocket.queue";

    /**
     * 会话更新路由键.
     */
    public static final String CONVERSATION_UPDATED_ROUTING_KEY = "union.talk.conversation.updated";

    /**
     * 私有构造方法，避免实例化.
     */
    private RabbitMqConstant() {
    }
}
