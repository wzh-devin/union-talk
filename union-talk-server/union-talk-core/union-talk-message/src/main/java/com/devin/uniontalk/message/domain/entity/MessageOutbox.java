package com.devin.uniontalk.message.domain.entity;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.datasource.handler.JsonbStringTypeHandler;
import com.devin.uniontalk.infrastructure.message.enums.MessageOutboxStatusEnum;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import lombok.Data;

/**
 * 2026/07/28 22:12.
 *
 * <p>
 * 消息事件发件箱实体
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_message_outbox", autoResultMap = true)
public class MessageOutbox implements Serializable {

    /**
     * 序列化版本号.
     */
    @Serial
    private static final long serialVersionUID = -1050707572806028045L;

    /**
     * 初始重试延迟毫秒数.
     */
    private static final long INITIAL_RETRY_DELAY_MILLIS = 1000L;

    /**
     * 最大重试延迟毫秒数.
     */
    private static final long MAX_RETRY_DELAY_MILLIS = 60000L;

    /**
     * 最大指数位数.
     */
    private static final int MAX_RETRY_EXPONENT = 6;

    /**
     * 发件箱记录id.
     */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /**
     * 事件id.
     */
    @TableField("event_id")
    private String eventId;

    /**
     * 聚合类型.
     */
    @TableField("aggregate_type")
    private String aggregateType;

    /**
     * 聚合id.
     */
    @TableField("aggregate_id")
    private BigInteger aggregateId;

    /**
     * 事件类型.
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 事件内容JSON.
     */
    @TableField(value = "payload_json", typeHandler = JsonbStringTypeHandler.class)
    private String payloadJson;

    /**
     * 发布状态.
     */
    @TableField("status")
    private String status;

    /**
     * 重试次数.
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 下次重试时间.
     */
    @TableField("next_retry_at")
    private Date nextRetryAt;

    /**
     * 发布时间.
     */
    @TableField("published_at")
    private Date publishedAt;

    /**
     * 创建时间.
     */
    @TableField("created_at")
    private Date createdAt;

    /**
     * 更新时间.
     */
    @TableField("updated_at")
    private Date updatedAt;

    /**
     * 初始化发件箱事件.
     *
     * @param eventId       事件id
     * @param aggregateType 聚合类型
     * @param aggregateId   聚合id
     * @param eventType     事件类型
     * @param event         事件内容
     */
    public void init(
            final String eventId,
            final String aggregateType,
            final BigInteger aggregateId,
            final String eventType,
            final Object event
    ) {
        Date now = new Date();
        this.id = IdGenerator.nextIdBigInteger();
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payloadJson = JSON.toJSONString(event, "iso8601");
        this.status = MessageOutboxStatusEnum.PENDING.name();
        this.retryCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 标记发布失败并计算下次重试时间.
     *
     * @param failedAt 失败时间
     */
    public void markPublishFailed(final Date failedAt) {
        int currentRetryCount = retryCount == null ? 0 : retryCount;
        int nextRetryCount = currentRetryCount + 1;
        int retryExponent = Math.min(currentRetryCount, MAX_RETRY_EXPONENT);
        long retryDelayMillis = Math.min(
                INITIAL_RETRY_DELAY_MILLIS << retryExponent,
                MAX_RETRY_DELAY_MILLIS
        );
        this.status = MessageOutboxStatusEnum.FAILED.name();
        this.retryCount = nextRetryCount;
        this.nextRetryAt = new Date(failedAt.getTime() + retryDelayMillis);
        this.updatedAt = failedAt;
    }
}
