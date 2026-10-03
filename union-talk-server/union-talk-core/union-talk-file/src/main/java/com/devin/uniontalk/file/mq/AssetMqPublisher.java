package com.devin.uniontalk.file.mq;

import com.devin.uniontalk.file.domain.entity.AssetOutbox;
import com.devin.uniontalk.infrastructure.file.enums.AssetOutboxEventTypeEnum;
import com.devin.uniontalk.rabbitmq.constant.RabbitMqConstant;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 2026/08/13 00:25.
 *
 * <p>资产发件箱RabbitMQ发布器</p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class AssetMqPublisher {

    /**
     * Broker发布确认超时时长.
     */
    private static final Duration PUBLISH_CONFIRM_TIMEOUT = Duration.ofSeconds(10);

    /** RabbitMQ模板. */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布资产事件并等待Broker确认.
     *
     * @param outbox 发件箱事件
     */
    public void publish(final AssetOutbox outbox) {
        CorrelationData correlationData = new CorrelationData(outbox.getEventId());
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(outbox.getEventId());
        properties.setTimestamp(new Date());
        Message message = new Message(
                outbox.getPayloadJson().getBytes(StandardCharsets.UTF_8),
                properties
        );
        rabbitTemplate.send(
                RabbitMqConstant.FILE_EXCHANGE,
                switch (AssetOutboxEventTypeEnum.valueOf(outbox.getEventType())) {
                    case ASSET_CONTENT_CHANGED -> RabbitMqConstant.ASSET_CONTENT_CHANGED_ROUTING_KEY;
                    case ASSET_DELETED -> RabbitMqConstant.ASSET_DELETED_ROUTING_KEY;
                },
                message,
                correlationData
        );
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(
                    PUBLISH_CONFIRM_TIMEOUT.toMillis(),
                    TimeUnit.MILLISECONDS
            );
            if (!confirm.isAck() || correlationData.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ资产事件未确认或未路由");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("RabbitMQ发布确认等待被中断", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("RabbitMQ资产事件发布确认失败", e);
        }
    }
}
