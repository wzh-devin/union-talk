package com.devin.uniontalk.message.domain.command;

import com.devin.uniontalk.message.domain.model.AgentCitation;
import java.math.BigInteger;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/07/28 22:12.
 *
 * <p>
 * Agent回复写回命令
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
public class AgentReplyCommand {

    /**
     * Agent运行id.
     */
    private BigInteger runId;

    /**
     * 会话id.
     */
    private BigInteger conversationId;

    /**
     * 触发消息id.
     */
    private BigInteger triggerMessageId;

    /**
     * 回复目标用户id.
     */
    private BigInteger replyToUserId;

    /**
     * 稳定Agent定义id.
     */
    private BigInteger agentId;

    /**
     * 模型标识.
     */
    private String modelId;

    /**
     * 回复内容.
     */
    private String content;

    /**
     * 引用信息列表.
     */
    private List<AgentCitation> citationList;

    /**
     * 幂等键.
     */
    private String idempotencyKey;
}
