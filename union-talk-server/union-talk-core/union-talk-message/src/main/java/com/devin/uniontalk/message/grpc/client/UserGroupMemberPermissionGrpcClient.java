package com.devin.uniontalk.message.grpc.client;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.exception.BizException;
import com.devin.uniontalk.grpc.user.domain.request.GetGroupAssetMemberContextRequest;
import com.devin.uniontalk.grpc.user.domain.response.GetGroupAssetMemberContextResponse;
import com.devin.uniontalk.grpc.user.service.UserGrpcServiceGrpc;
import com.devin.uniontalk.infrastructure.message.enums.AgentConversationRoleEnum;
import com.devin.uniontalk.infrastructure.message.enums.ConversationTypeEnum;
import com.devin.uniontalk.infrastructure.user.enums.GroupMemberRoleEnum;
import com.devin.uniontalk.message.domain.model.AgentConversationPermission;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * 2026/07/31 13:25.
 *
 * <p>
 * User群成员权限Grpc客户端
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
public class UserGroupMemberPermissionGrpcClient {

    /**
     * 用户Grpc阻塞调用客户端.
     */
    @GrpcClient("union-talk-user")
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userGrpcServiceBlockingStub;

    /**
     * 查询群成员权限.
     *
     * @param groupId 群聊id
     * @param userId  用户id
     * @return 当前群成员权限
     */
    public AgentConversationPermission getPermission(
            final BigInteger groupId,
            final BigInteger userId
    ) {
        try {
            GetGroupAssetMemberContextResponse response = userGrpcServiceBlockingStub
                    .getGroupAssetMemberContext(GetGroupAssetMemberContextRequest.newBuilder()
                            .setGroupId(groupId.toString())
                            .setOperatorUserId(userId.toString())
                            .build());
            if (!response.getBr().getSuccess()) {
                log.warn(
                        "User查询群成员权限失败, groupId={}, userId={}, code={}, message={}",
                        groupId,
                        userId,
                        response.getBr().getCode(),
                        response.getBr().getMessage()
                );
                throw new BizException(BizErrorEnum.CONVERSATION_TARGET_QUERY_FAILED);
            }
            if (!response.getActiveMember()) {
                return AgentConversationPermission.nonMember();
            }
            GroupMemberRoleEnum role = GroupMemberRoleEnum.valueOf(response.getRole());
            return AgentConversationPermission.builder()
                    .member(Boolean.TRUE)
                    .role(AgentConversationRoleEnum.valueOf(role.name()))
                    .conversationType(ConversationTypeEnum.GROUP)
                    .groupId(groupId)
                    .build();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用User查询群成员权限Grpc失败, groupId={}, userId={}", groupId, userId, e);
            throw new BizException(BizErrorEnum.CONVERSATION_TARGET_QUERY_FAILED);
        }
    }
}
