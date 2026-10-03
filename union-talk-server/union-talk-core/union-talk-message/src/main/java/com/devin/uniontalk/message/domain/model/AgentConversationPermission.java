package com.devin.uniontalk.message.domain.model;

import com.devin.uniontalk.infrastructure.message.enums.AgentConversationRoleEnum;
import com.devin.uniontalk.infrastructure.message.enums.ConversationTypeEnum;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/07/31 13:20.
 *
 * <p>
 * 当前用户的会话Agent权限
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
public class AgentConversationPermission {

    /**
     * 是否为当前有效会话成员.
     */
    private Boolean member;

    /**
     * 当前会话角色，非成员时为空.
     */
    private AgentConversationRoleEnum role;

    /**
     * 会话类型，非成员时为空.
     */
    private ConversationTypeEnum conversationType;

    /**
     * 群聊id，私聊或非成员时为空.
     */
    private BigInteger groupId;

    /**
     * 构建非成员权限.
     *
     * @return 非成员权限
     */
    public static AgentConversationPermission nonMember() {
        return AgentConversationPermission.builder()
                .member(Boolean.FALSE)
                .build();
    }
}
