package com.devin.uniontalk.user.mq.publisher;

import com.alibaba.fastjson2.JSON;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.infrastructure.user.enums.FriendRequestStatusEnum;
import com.devin.uniontalk.rabbitmq.constant.RabbitMqConstant;
import com.devin.uniontalk.rabbitmq.domain.event.FriendRequestAcceptedEvent;
import com.devin.uniontalk.user.domain.entity.FriendRequest;
import com.devin.uniontalk.user.domain.entity.User;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 2026/05/20 18:30.
 *
 * <p>
 * 好友申请同意 MQ 发布器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FriendRequestAcceptedMqPublisher {

    /**
     * RabbitMQ 模板.
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布好友申请同意事件.
     *
     * @param request        好友申请记录
     * @param toUser         同意用户
     * @param conversationId 会话id
     */
    public void publishAccepted(
            final FriendRequest request,
            final User toUser,
            final BigInteger conversationId
    ) {
        FriendRequestAcceptedEvent event = FriendRequestAcceptedEvent.builder()
                .eventId(IdGenerator.nextKey())
                .friendRequestId(request.getId())
                .fromUserId(request.getFromUserId())
                .toUserId(request.getToUserId())
                .toUsername(toUser.getUsername())
                .toAvatarUrl(toUser.getAvatarUrl())
                .conversationId(conversationId)
                .status(FriendRequestStatusEnum.valueOf(request.getStatus()))
                .handledAt(request.getHandledAt())
                .content(toUser.getUsername() + " 已同意你的好友申请，开始聊天吧")
                .build();

        Message message = new Message(JSON.toJSONBytes(event), buildMessageProperties(event.getEventId()));
        rabbitTemplate.send(
                RabbitMqConstant.FRIEND_REQUEST_ACCEPTED_EXCHANGE,
                RabbitMqConstant.FRIEND_REQUEST_ACCEPTED_ROUTING_KEY,
                message
        );
        log.info(
                "发送好友申请同意MQ事件成功, eventId={}, friendRequestId={}, fromUserId={}",
                event.getEventId(),
                event.getFriendRequestId(),
                event.getFromUserId()
        );
    }

    /**
     * 构建 MQ 消息属性.
     *
     * @param eventId 事件id
     * @return MQ 消息属性
     */
    private MessageProperties buildMessageProperties(final String eventId) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        messageProperties.setMessageId(eventId);
        messageProperties.setTimestamp(new Date());
        return messageProperties;
    }
}
