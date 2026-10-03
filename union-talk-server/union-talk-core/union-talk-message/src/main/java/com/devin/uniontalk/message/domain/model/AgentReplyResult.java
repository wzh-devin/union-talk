package com.devin.uniontalk.message.domain.model;

import java.math.BigInteger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/07/28 22:12.
 *
 * <p>
 * Agent回复写回结果
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
public class AgentReplyResult {

    /**
     * 正式回复消息id.
     */
    private BigInteger answerMessageId;

    /**
     * 是否本次创建.
     */
    private Boolean created;
}
