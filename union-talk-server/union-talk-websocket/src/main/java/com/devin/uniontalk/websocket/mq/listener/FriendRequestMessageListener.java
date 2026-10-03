package com.devin.uniontalk.websocket.mq.listener;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.devin.uniontalk.rabbitmq.constant.RabbitMqConstant;
import com.devin.uniontalk.rabbitmq.domain.event.FriendRequestAcceptedEvent;
import com.devin.uniontalk.rabbitmq.domain.event.FriendRequestCreatedEvent;
import com.devin.uniontalk.websocket.service.WebSocketConnectionService;
import com.devin.uniontalk.websocket.service.adapter.WsFrameAdapter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 2026/05/20 00:00.
 *
 * <p>
 * 好友申请 WebSocket 消息监听器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FriendRequestMessageListener {

    /**
     * WebSocket 连接服务.
     */
    private final WebSocketConnectionService webSocketConnectionService;

    /**
     * 监听好友申请创建事件并推送给目标用户.
     *
     * @param message RabbitMQ 消息
     */
    @RabbitListener(queues = RabbitMqConstant.FRIEND_REQUEST_WEBSOCKET_QUEUE)
    public void onFriendRequestCreated(final Message message) {
        FriendRequestCreatedEvent event = parseEvent(message, FriendRequestCreatedEvent.class, "好友申请");
        if (Objects.isNull(event)) {
            return;
        }
        if (Objects.isNull(event.getToUserId())) {
            log.warn("好友申请MQ事件缺少目标用户, eventId={}", event.getEventId());
            return;
        }
        int sentConnectionCount = webSocketConnectionService.sendToUser(
                event.getToUserId(),
                WsFrameAdapter.friendRequestCreated(event)
        );
        log.info(
                "好友申请WebSocket分发完成, eventId={}, friendRequestId={}, "
                        + "toUserId={}, sentConnectionCount={}",
                event.getEventId(),
                event.getFriendRequestId(),
                event.getToUserId(),
                sentConnectionCount
        );
    }

    /**
     * 监听好友申请同意事件并推送给申请用户.
     *
     * @param message RabbitMQ 消息
     */
    @RabbitListener(queues = RabbitMqConstant.FRIEND_REQUEST_ACCEPTED_WEBSOCKET_QUEUE)
    public void onFriendRequestAccepted(final Message message) {
        FriendRequestAcceptedEvent event = parseEvent(message, FriendRequestAcceptedEvent.class, "好友申请同意");
        if (Objects.isNull(event)) {
            return;
        }
        if (Objects.isNull(event.getFromUserId())) {
            log.warn("好友申请同意MQ事件缺少申请用户, eventId={}", event.getEventId());
            return;
        }
        int sentConnectionCount = webSocketConnectionService.sendToUser(
                event.getFromUserId(),
                WsFrameAdapter.friendRequestAccepted(event)
        );
        log.info(
                "好友申请同意WebSocket分发完成, eventId={}, friendRequestId={}, "
                        + "fromUserId={}, sentConnectionCount={}",
                event.getEventId(),
                event.getFriendRequestId(),
                event.getFromUserId(),
                sentConnectionCount
        );
    }

    /**
     * 解析好友申请事件.
     *
     * @param message   RabbitMQ 消息
     * @param eventType 事件类型
     * @param scene     事件场景
     * @param <T>       事件泛型
     * @return 好友申请事件
     */
    private <T> T parseEvent(final Message message, final Class<T> eventType, final String scene) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            return JSON.parseObject(body, eventType);
        } catch (JSONException e) {
            log.warn("解析{}MQ事件失败: {}", scene, e.getMessage());
            return null;
        }
    }
}
