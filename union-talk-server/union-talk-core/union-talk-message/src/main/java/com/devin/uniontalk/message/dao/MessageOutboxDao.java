package com.devin.uniontalk.message.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.infrastructure.message.enums.MessageOutboxAggregateTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageOutboxEventTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageOutboxStatusEnum;
import com.devin.uniontalk.message.domain.entity.MessageOutbox;
import com.devin.uniontalk.message.mapper.MessageOutboxMapper;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2026/07/28 22:15.
 *
 * <p>
 * 消息事件发件箱Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageOutboxDao extends ServiceImpl<MessageOutboxMapper, MessageOutbox> {

    /**
     * 发布租约时长.
     */
    private static final Duration PUBLISH_LEASE_DURATION = Duration.ofMinutes(5);

    /**
     * 创建发件箱事件.
     *
     * @param eventId       事件id
     * @param aggregateType 聚合类型
     * @param aggregateId   聚合id
     * @param eventType     事件类型
     * @param event         事件内容
     * @return 发件箱事件
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public MessageOutbox createEvent(
            final String eventId,
            final MessageOutboxAggregateTypeEnum aggregateType,
            final BigInteger aggregateId,
            final MessageOutboxEventTypeEnum eventType,
            final Object event
    ) {
        getBaseMapper().lockEvent(eventId);
        MessageOutbox existsOutbox = lambdaQuery()
                .eq(MessageOutbox::getEventId, eventId)
                .last("LIMIT 1")
                .one();
        if (Objects.nonNull(existsOutbox)) {
            return existsOutbox;
        }
        MessageOutbox outbox = new MessageOutbox();
        outbox.init(eventId, aggregateType.name(), aggregateId, eventType.name(), event);
        boolean saved = save(outbox);
        if (!saved) {
            throw new IllegalStateException("创建消息发件箱事件失败");
        }
        return outbox;
    }

    /**
     * 认领可发布事件列表.
     *
     * @param batchSize 批量数量
     * @return 发件箱事件列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<MessageOutbox> claimPublishableList(final Integer batchSize) {
        Date staleBefore = new Date(System.currentTimeMillis() - PUBLISH_LEASE_DURATION.toMillis());
        return getBaseMapper().claimPublishableList(
                MessageOutboxStatusEnum.PENDING.name(),
                MessageOutboxStatusEnum.FAILED.name(),
                MessageOutboxStatusEnum.PUBLISHING.name(),
                staleBefore,
                batchSize
        );
    }

    /**
     * 标记事件发布成功.
     *
     * @param outbox 发件箱事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void markPublished(final MessageOutbox outbox) {
        getBaseMapper().markPublished(
                outbox.getId(),
                MessageOutboxStatusEnum.PUBLISHING.name(),
                MessageOutboxStatusEnum.PUBLISHED.name(),
                new Date()
        );
    }

    /**
     * 标记事件发布失败.
     *
     * @param outbox 发件箱事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(final MessageOutbox outbox) {
        outbox.markPublishFailed(new Date());
        getBaseMapper().markFailed(outbox, MessageOutboxStatusEnum.PUBLISHING.name());
    }
}
