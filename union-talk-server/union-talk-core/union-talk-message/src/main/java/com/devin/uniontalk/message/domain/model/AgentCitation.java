package com.devin.uniontalk.message.domain.model;

import com.devin.uniontalk.infrastructure.message.enums.AgentCitationSourceTypeEnum;
import java.math.BigInteger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/07/28 22:12.
 *
 * <p>
 * Agent引用信息
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
public class AgentCitation {

    /**
     * 引用标识.
     */
    private String citationKey;

    /**
     * 引用来源类型.
     */
    private AgentCitationSourceTypeEnum sourceType;

    /**
     * 消息id.
     */
    private BigInteger messageId;

    /**
     * 资产文件id.
     */
    private BigInteger assetFileId;

    /**
     * 资源版本.
     */
    private Integer resourceVersion;

    /**
     * 资源块id.
     */
    private BigInteger chunkId;

    /**
     * 起始页码.
     */
    private Integer pageFrom;

    /**
     * 结束页码.
     */
    private Integer pageTo;

    /**
     * 标题路径.
     */
    private String headingPath;
}
