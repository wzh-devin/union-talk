package com.devin.uniontalk.message.domain.model;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/07/31 13:20.
 *
 * <p>
 * Agent回答使用的紧凑会话上下文
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
public class AgentConversationContext {

    /**
     * 会话id.
     */
    private BigInteger conversationId;

    /**
     * 触发消息id.
     */
    private BigInteger triggerMessageId;

    /**
     * 当前问题.
     */
    private String question;

    /**
     * 最近文本消息列表.
     */
    private List<AgentConversationMessage> recentMessageList;

    /**
     * 当前有效会话成员id列表.
     */
    private List<BigInteger> receiverUserIdList;

    /**
     * 被引用的消息id.
     */
    private BigInteger quotedMessageId;

    /**
     * 当前问题明确引用的资源id列表.
     */
    private List<BigInteger> referencedResourceIdList;

    /**
     * 当前问题明确引用的资产文件id列表.
     */
    private List<BigInteger> referencedAssetFileIdList;
}
