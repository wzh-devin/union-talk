package com.devin.uniontalk.file.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.file.domain.entity.AssetFile;
import com.devin.uniontalk.file.domain.entity.AssetOutbox;
import com.devin.uniontalk.file.mapper.AssetOutboxMapper;
import com.devin.uniontalk.infrastructure.file.constant.AssetOutboxConstant;
import com.devin.uniontalk.infrastructure.file.enums.AssetOutboxEventTypeEnum;
import com.devin.uniontalk.infrastructure.file.enums.AssetOutboxStatusEnum;
import com.devin.uniontalk.rabbitmq.constant.RabbitMqConstant;
import com.devin.uniontalk.rabbitmq.domain.event.AssetDeletedEvent;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2026/08/13 00:20.
 *
 * <p>资产事件发件箱Dao</p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
public class AssetOutboxDao extends ServiceImpl<AssetOutboxMapper, AssetOutbox> {

    /**
     * 发布中事件的认领租约时长.
     */
    private static final Duration PUBLISH_LEASE_DURATION = Duration.ofMinutes(5);

    /**
     * 事务内创建资产删除事件.
     *
     * @param assetFile  资产文件
     * @param occurredAt 事件发生时间
     * @return 发件箱事件
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AssetOutbox createDeletedEvent(final AssetFile assetFile, final Date occurredAt) {
        String eventId = AssetOutboxConstant.ASSET_DELETED_EVENT_ID_PREFIX + assetFile.getId()
                + ":" + assetFile.getResourceVersion();
        AssetDeletedEvent event = AssetDeletedEvent.builder()
                .eventId(eventId)
                .eventType(AssetOutboxEventTypeEnum.ASSET_DELETED)
                .schemaVersion(RabbitMqConstant.ASSET_DELETED_SCHEMA_VERSION)
                .assetFileId(assetFile.getId())
                .conversationId(assetFile.getConversationId())
                .resourceVersion(assetFile.getResourceVersion())
                .occurredAt(occurredAt)
                .build();
        return createEvent(
                eventId,
                assetFile.getId(),
                assetFile.getResourceVersion(),
                AssetOutboxEventTypeEnum.ASSET_DELETED,
                event
        );
    }

    /**
     * 事务内创建资产事件.
     *
     * @param eventId 事件id
     * @param assetFileId 资产文件id
     * @param resourceVersion 内容版本
     * @param eventType 事件类型
     * @param event 事件内容
     * @return 发件箱事件
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AssetOutbox createEvent(
            final String eventId,
            final BigInteger assetFileId,
            final Integer resourceVersion,
            final AssetOutboxEventTypeEnum eventType,
            final Object event
    ) {
        getBaseMapper().lockEvent(eventId);
        AssetOutbox existing = lambdaQuery().eq(AssetOutbox::getEventId, eventId).last("LIMIT 1").one();
        if (Objects.nonNull(existing)) {
            return existing;
        }
        AssetOutbox outbox = new AssetOutbox();
        outbox.init(eventId, assetFileId, resourceVersion, eventType.name(), event);
        if (!save(outbox)) {
            throw new IllegalStateException("创建资产发件箱事件失败");
        }
        return outbox;
    }

    /**
     * 认领可发布事件.
     *
     * @param batchSize 单次认领数量
     * @return 已取得发布租约的事件列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<AssetOutbox> claimPublishableList(final Integer batchSize) {
        Date staleBefore = new Date(System.currentTimeMillis() - PUBLISH_LEASE_DURATION.toMillis());
        return getBaseMapper().claimPublishableList(
                AssetOutboxStatusEnum.PENDING.name(),
                AssetOutboxStatusEnum.FAILED.name(),
                AssetOutboxStatusEnum.PUBLISHING.name(),
                staleBefore,
                batchSize
        );
    }

    /**
     * 标记发布成功.
     *
     * @param outbox 发件箱事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void markPublished(final AssetOutbox outbox) {
        getBaseMapper().markPublished(
                outbox.getId(),
                AssetOutboxStatusEnum.PUBLISHING.name(),
                AssetOutboxStatusEnum.PUBLISHED.name(),
                new Date()
        );
    }

    /**
     * 标记发布失败.
     *
     * @param outbox 发件箱事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(final AssetOutbox outbox) {
        outbox.markPublishFailed(new Date());
        getBaseMapper().markFailed(outbox, AssetOutboxStatusEnum.PUBLISHING.name());
    }
}
