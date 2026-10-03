package com.devin.uniontalk.file.domain.entity;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.datasource.handler.JsonbStringTypeHandler;
import com.devin.uniontalk.infrastructure.file.constant.AssetOutboxConstant;
import com.devin.uniontalk.infrastructure.file.enums.AssetOutboxStatusEnum;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import lombok.Data;

/**
 * 2026/08/13 00:15.
 *
 * <p>
 * 资产事件发件箱实体
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_asset_outbox", autoResultMap = true)
public class AssetOutbox implements Serializable {

    @Serial
    private static final long serialVersionUID = 4511239022928001200L;

    /** 发件箱记录id. */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 事件id. */
    @TableField("event_id")
    private String eventId;

    /** 资产文件id. */
    @TableField("asset_file_id")
    private BigInteger assetFileId;

    /** 内容版本. */
    @TableField("resource_version")
    private Integer resourceVersion;

    /** 事件类型. */
    @TableField("event_type")
    private String eventType;

    /** 事件内容JSON. */
    @TableField(value = "payload_json", typeHandler = JsonbStringTypeHandler.class)
    private String payloadJson;

    /** 发布状态. */
    @TableField("status")
    private String status;

    /** 重试次数. */
    @TableField("retry_count")
    private Integer retryCount;

    /** 下次重试时间. */
    @TableField("next_retry_at")
    private Date nextRetryAt;

    /** 发布时间. */
    @TableField("published_at")
    private Date publishedAt;

    /** 创建时间. */
    @TableField("created_at")
    private Date createdAt;

    /** 更新时间. */
    @TableField("updated_at")
    private Date updatedAt;

    /**
     * 初始化资产事件.
     *
     * @param eventId        事件id
     * @param assetFileId    资产文件id
     * @param resourceVersion 内容版本
     * @param eventType      事件类型
     * @param event          事件内容
     */
    public void init(
            final String eventId,
            final BigInteger assetFileId,
            final Integer resourceVersion,
            final String eventType,
            final Object event
    ) {
        Date now = new Date();
        this.id = IdGenerator.nextIdBigInteger();
        this.eventId = eventId;
        this.assetFileId = assetFileId;
        this.resourceVersion = resourceVersion;
        this.eventType = eventType;
        this.payloadJson = JSON.toJSONString(event, "iso8601");
        this.status = AssetOutboxStatusEnum.PENDING.name();
        this.retryCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 标记发布失败并安排指数退避重试.
     *
     * @param failedAt 失败时间
     */
    public void markPublishFailed(final Date failedAt) {
        int currentRetryCount = retryCount == null ? 0 : retryCount;
        long delayMillis = Math.min(
                AssetOutboxConstant.INITIAL_RETRY_DELAY_MILLIS
                        << Math.min(currentRetryCount, AssetOutboxConstant.MAX_BACKOFF_SHIFT),
                AssetOutboxConstant.MAX_RETRY_DELAY_MILLIS
        );
        this.status = AssetOutboxStatusEnum.FAILED.name();
        this.retryCount = currentRetryCount + 1;
        this.nextRetryAt = new Date(failedAt.getTime() + delayMillis);
        this.updatedAt = failedAt;
    }
}
