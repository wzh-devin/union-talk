package com.devin.uniontalk.message.domain.model;

import com.devin.uniontalk.infrastructure.message.enums.ConversationTypeEnum;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/07/22 10:40.
 *
 * <p>
 * 会话资产上下文
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
public class ConversationAssetContext {

    /**
     * 会话id.
     */
    private BigInteger conversationId;

    /**
     * 会话类型.
     */
    private ConversationTypeEnum conversationType;

    /**
     * 群聊id.
     */
    private BigInteger groupId;

    /**
     * 操作用户是否为私聊成员.
     */
    private Boolean privateMember;
}
