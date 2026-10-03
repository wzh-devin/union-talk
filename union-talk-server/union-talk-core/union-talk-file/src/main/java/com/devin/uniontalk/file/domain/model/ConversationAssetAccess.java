package com.devin.uniontalk.file.domain.model;

import com.devin.uniontalk.infrastructure.message.enums.ConversationTypeEnum;
import com.devin.uniontalk.infrastructure.user.enums.GroupMemberRoleEnum;
import java.math.BigInteger;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/07/22 11:00.
 *
 * <p>
 * 会话资产访问上下文
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
public class ConversationAssetAccess {

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
     * 操作用户id.
     */
    private BigInteger operatorUserId;

    /**
     * 群成员角色.
     */
    private GroupMemberRoleEnum groupRole;

    /**
     * 是否为有效成员.
     */
    private Boolean activeMember;

    /**
     * 判断是否群主.
     *
     * @return true表示群主
     */
    public boolean isGroupOwner() {
        return ConversationTypeEnum.GROUP == conversationType
                && GroupMemberRoleEnum.OWNER == groupRole;
    }

    /**
     * 判断是否可以写入指定成员空间.
     *
     * @param ownerUserId 空间所有者用户id
     * @return true表示可以写入
     */
    public boolean canWrite(final BigInteger ownerUserId) {
        return isGroupOwner() || Objects.equals(operatorUserId, ownerUserId);
    }
}
