package com.devin.uniontalk.rabbitmq.domain.event;

import com.devin.uniontalk.infrastructure.file.enums.AssetOutboxEventTypeEnum;
import java.math.BigInteger;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/08/13 00:15.
 *
 * <p>
 * 资产内容变更事件
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetContentChangedEvent {

    /** 事件id. */
    private String eventId;

    /** 事件类型. */
    private AssetOutboxEventTypeEnum eventType;

    /** Schema版本. */
    private Integer schemaVersion;

    /** 资产文件id. */
    private BigInteger assetFileId;

    /** 会话id. */
    private BigInteger conversationId;

    /** 内容版本. */
    private Integer resourceVersion;

    /** 事件时间. */
    private Date occurredAt;
}
