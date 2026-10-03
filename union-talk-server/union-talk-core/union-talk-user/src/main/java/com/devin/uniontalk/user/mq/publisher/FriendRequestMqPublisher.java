package com.devin.uniontalk.user.mq.publisher;

import com.alibaba.fastjson2.JSON;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.infrastructure.user.enums.FriendRequestStatusEnum;
import com.devin.uniontalk.rabbitmq.constant.RabbitMqConstant;
import com.devin.uniontalk.rabbitmq.domain.event.FriendRequestCreatedEvent;
import com.devin.uniontalk.user.domain.entity.FriendRequest;
import com.devin.uniontalk.user.domain.entity.User;
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
 * 2026/05/20 00:00.
 *
 * <p>
 * 好友申请 MQ 发布器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FriendRequestMqPublisher {

    /**
     * RabbitMQ 模板.
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布好友申请创建事件.
     *
     * @param request  好友申请记录
     * @param fromUser 申请用户
     */
    public void publishCreated(final FriendRequest request, final User fromUser) {
        // 创建好友申请创建事件
        FriendRequestCreatedEvent event = FriendRequestCreatedEvent.builder()
                .eventId(IdGenerator.nextKey())
                .friendRequestId(request.getId())
                .toUserId(request.getToUserId())
                .fromUserId(request.getFromUserId())
                .status(FriendRequestStatusEnum.valueOf(request.getStatus()))
                .createdAt(request.getCreatedAt())
                .applyMsg(request.getApplyMsg())
                .fromAvatarUrl(fromUser.getAvatarUrl())
                .fromUsername(fromUser.getUsername())
                .build();

        // 创建消息属性
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        messageProperties.setMessageId(event.getEventId());
        messageProperties.setTimestamp(new Date());

        Message message = new Message(JSON.toJSONBytes(event), messageProperties);
        rabbitTemplate.send(
                RabbitMqConstant.FRIEND_REQUEST_EXCHANGE,
                RabbitMqConstant.FRIEND_REQUEST_ROUTING_KEY,
                message
        );
        log.info(
                "发送好友申请MQ事件成功, eventId={}, friendRequestId={}, toUserId={}",
                event.getEventId(),
                event.getFriendRequestId(),
                event.getToUserId()
        );
    }
}
