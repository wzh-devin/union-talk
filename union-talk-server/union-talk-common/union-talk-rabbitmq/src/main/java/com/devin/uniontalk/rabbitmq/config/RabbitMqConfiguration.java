package com.devin.uniontalk.rabbitmq.config;

import com.devin.uniontalk.rabbitmq.constant.RabbitMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 2026/05/20 00:00.
 *
 * <p>
 * RabbitMQ 自动配置入口
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@AutoConfiguration
public class RabbitMqConfiguration {

    /**
     * 队列最大消息数量.
     */
    private static final Long QUEUE_MAX_LENGTH = 1000L;

    /**
     * 创建好友申请 WebSocket 推送队列.
     *
     * @return 好友申请 WebSocket 推送队列
     */
    @Bean
    public Queue friendRequestWebsocketQueue() {
        return QueueBuilder.durable(RabbitMqConstant.FRIEND_REQUEST_WEBSOCKET_QUEUE)
                .maxLength(QUEUE_MAX_LENGTH)
                .build();
    }

    /**
     * 创建好友申请同意 WebSocket 推送队列.
     *
     * @return 好友申请同意 WebSocket 推送队列
     */
    @Bean
    public Queue friendRequestAcceptedWebsocketQueue() {
        return QueueBuilder.durable(RabbitMqConstant.FRIEND_REQUEST_ACCEPTED_WEBSOCKET_QUEUE)
                .maxLength(QUEUE_MAX_LENGTH)
                .build();
    }

    /**
     * 创建消息创建 WebSocket 推送队列.
     *
     * @return 消息创建 WebSocket 推送队列
     */
    @Bean
    public Queue messageCreatedWebsocketQueue() {
        return QueueBuilder.durable(RabbitMqConstant.MESSAGE_CREATED_WEBSOCKET_QUEUE)
                .maxLength(QUEUE_MAX_LENGTH)
                .build();
    }

    /**
     * 创建Agent提及队列.
     *
     * @return Agent提及队列
     */
    @Bean
    public Queue agentMentionedQueue() {
        return QueueBuilder.durable(RabbitMqConstant.AGENT_MENTIONED_QUEUE).build();
    }

    /**
     * 创建会话更新 WebSocket 推送队列.
     *
     * @return 会话更新 WebSocket 推送队列
     */
    @Bean
    public Queue conversationUpdatedWebsocketQueue() {
        return QueueBuilder.durable(RabbitMqConstant.CONVERSATION_UPDATED_WEBSOCKET_QUEUE)
                .maxLength(QUEUE_MAX_LENGTH)
                .build();
    }

    /**
     * 创建好友申请交换机.
     *
     * @return 好友申请交换机
     */
    @Bean
    public TopicExchange friendRequestExchange() {
        return new TopicExchange(RabbitMqConstant.FRIEND_REQUEST_EXCHANGE, Boolean.TRUE, Boolean.FALSE);
    }

    /**
     * 创建好友申请同意交换机.
     *
     * @return 好友申请同意交换机
     */
    @Bean
    public TopicExchange friendRequestAcceptedExchange() {
        return new TopicExchange(RabbitMqConstant.FRIEND_REQUEST_ACCEPTED_EXCHANGE, Boolean.TRUE, Boolean.FALSE);
    }

    /**
     * 创建消息事件交换机.
     *
     * @return 消息事件交换机
     */
    @Bean
    public TopicExchange messageExchange() {
        return new TopicExchange(RabbitMqConstant.MESSAGE_EXCHANGE, Boolean.TRUE, Boolean.FALSE);
    }

    /**
     * 创建文件事件交换机.
     *
     * @return 文件事件交换机
     */
    @Bean
    public TopicExchange fileExchange() {
        return new TopicExchange(RabbitMqConstant.FILE_EXCHANGE, Boolean.TRUE, Boolean.FALSE);
    }

    /**
     * 创建会话事件交换机.
     *
     * @return 会话事件交换机
     */
    @Bean
    public TopicExchange conversationExchange() {
        return new TopicExchange(RabbitMqConstant.CONVERSATION_EXCHANGE, Boolean.TRUE, Boolean.FALSE);
    }

    /**
     * 绑定好友申请交换机与 WebSocket 推送队列.
     *
     * @return 好友申请 WebSocket 推送绑定关系
     */
    @Bean
    public Binding friendRequestWebsocketBinding() {
        return BindingBuilder.bind(friendRequestWebsocketQueue())
                .to(friendRequestExchange())
                .with(RabbitMqConstant.FRIEND_REQUEST_ROUTING_KEY);
    }

    /**
     * 绑定好友申请同意交换机与 WebSocket 推送队列.
     *
     * @return 好友申请同意 WebSocket 推送绑定关系
     */
    @Bean
    public Binding friendRequestAcceptedWebsocketBinding() {
        return BindingBuilder.bind(friendRequestAcceptedWebsocketQueue())
                .to(friendRequestAcceptedExchange())
                .with(RabbitMqConstant.FRIEND_REQUEST_ACCEPTED_ROUTING_KEY);
    }

    /**
     * 绑定消息交换机与 WebSocket 推送队列.
     *
     * @return 消息创建 WebSocket 推送绑定关系
     */
    @Bean
    public Binding messageCreatedWebsocketBinding() {
        return BindingBuilder.bind(messageCreatedWebsocketQueue())
                .to(messageExchange())
                .with(RabbitMqConstant.MESSAGE_CREATED_ROUTING_KEY);
    }

    /**
     * 绑定消息交换机与Agent提及队列.
     *
     * @return Agent提及绑定关系
     */
    @Bean
    public Binding agentMentionedBinding() {
        return BindingBuilder.bind(agentMentionedQueue())
                .to(messageExchange())
                .with(RabbitMqConstant.AGENT_MENTIONED_ROUTING_KEY);
    }

    /**
     * 绑定会话交换机与 WebSocket 推送队列.
     *
     * @return 会话更新 WebSocket 推送绑定关系
     */
    @Bean
    public Binding conversationUpdatedWebsocketBinding() {
        return BindingBuilder.bind(conversationUpdatedWebsocketQueue())
                .to(conversationExchange())
                .with(RabbitMqConstant.CONVERSATION_UPDATED_ROUTING_KEY);
    }
}
