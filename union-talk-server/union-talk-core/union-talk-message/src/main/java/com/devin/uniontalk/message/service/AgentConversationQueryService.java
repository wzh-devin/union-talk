package com.devin.uniontalk.message.service;

import com.devin.uniontalk.message.domain.model.AgentConversationContext;
import com.devin.uniontalk.message.domain.model.AgentConversationPermission;
import java.math.BigInteger;

/**
 * 2026/07/31 13:30.
 *
 * <p>
 * Agent会话查询服务
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface AgentConversationQueryService {

    /**
     * 查询Agent回答使用的紧凑会话上下文.
     *
     * @param conversationId    会话id
     * @param triggerMessageId  触发消息id
     * @param recentMessageLimit 最近消息数量上限
     * @return Agent紧凑会话上下文
     */
    AgentConversationContext getContext(
            BigInteger conversationId,
            BigInteger triggerMessageId,
            Integer recentMessageLimit
    );

    /**
     * 查询当前用户的会话Agent权限.
     *
     * @param conversationId 会话id
     * @param userId         用户id
     * @return 当前会话Agent权限
     */
    AgentConversationPermission getPermission(BigInteger conversationId, BigInteger userId);
}
