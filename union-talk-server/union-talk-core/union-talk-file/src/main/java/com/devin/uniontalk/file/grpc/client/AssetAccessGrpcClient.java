package com.devin.uniontalk.file.grpc.client;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.exception.BizException;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.file.domain.model.ConversationAssetAccess;
import com.devin.uniontalk.grpc.base.BaseResponse;
import com.devin.uniontalk.grpc.message.domain.request.GetConversationAssetContextRequest;
import com.devin.uniontalk.grpc.message.domain.response.GetConversationAssetContextResponse;
import com.devin.uniontalk.grpc.message.service.ConversationGrpcServiceGrpc;
import com.devin.uniontalk.grpc.user.domain.model.ConversationUserInfo;
import com.devin.uniontalk.grpc.user.domain.request.ConversationTargetInfoRequest;
import com.devin.uniontalk.grpc.user.domain.request.GetGroupAssetMemberContextRequest;
import com.devin.uniontalk.grpc.user.domain.response.ConversationTargetInfoResponse;
import com.devin.uniontalk.grpc.user.domain.response.GetGroupAssetMemberContextResponse;
import com.devin.uniontalk.grpc.user.service.UserGrpcServiceGrpc;
import com.devin.uniontalk.infrastructure.message.enums.ConversationTypeEnum;
import com.devin.uniontalk.infrastructure.user.enums.GroupMemberRoleEnum;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * 2026/07/22 11:05.
 *
 * <p>
 * 会话资产访问上下文 Grpc 客户端
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
public class AssetAccessGrpcClient {

    /**
     * 消息会话 Grpc 阻塞调用客户端.
     */
    @GrpcClient("union-talk-message")
    private ConversationGrpcServiceGrpc.ConversationGrpcServiceBlockingStub conversationGrpcServiceBlockingStub;

    /**
     * 用户 Grpc 阻塞调用客户端.
     */
    @GrpcClient("union-talk-user")
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userGrpcServiceBlockingStub;

    /**
     * 查询会话资产访问上下文.
     *
     * @param conversationId 会话id
     * @param operatorUserId 操作用户id
     * @return 会话资产访问上下文
     */
    public ConversationAssetAccess getAccess(
            final BigInteger conversationId,
            final BigInteger operatorUserId
    ) {
        try {
            GetConversationAssetContextResponse conversationResponse = conversationGrpcServiceBlockingStub
                    .getConversationAssetContext(GetConversationAssetContextRequest.newBuilder()
                            .setConversationId(conversationId.toString())
                            .setOperatorUserId(operatorUserId.toString())
                            .build());
            validateBaseResponse(conversationResponse.getBr(), conversationId, operatorUserId);

            if (ConversationTypeEnum.PRIVATE.name().equals(conversationResponse.getConversationType())) {
                AssertUtils.isTrue(
                        conversationResponse.getPrivateMember(),
                        BizErrorEnum.NOT_CONVERSATION_MEMBER
                );
                return ConversationAssetAccess.builder()
                        .conversationId(conversationId)
                        .conversationType(ConversationTypeEnum.PRIVATE)
                        .operatorUserId(operatorUserId)
                        .groupRole(GroupMemberRoleEnum.MEMBER)
                        .activeMember(Boolean.TRUE)
                        .build();
            }
            AssertUtils.isTrue(
                    ConversationTypeEnum.GROUP.name().equals(conversationResponse.getConversationType()),
                    BizErrorEnum.ASSET_ACCESS_CONTEXT_QUERY_FAILED
            );
            BigInteger groupId = new BigInteger(conversationResponse.getGroupId());
            GetGroupAssetMemberContextResponse memberResponse = userGrpcServiceBlockingStub
                    .getGroupAssetMemberContext(GetGroupAssetMemberContextRequest.newBuilder()
                            .setGroupId(groupId.toString())
                            .setOperatorUserId(operatorUserId.toString())
                            .build());
            validateBaseResponse(memberResponse.getBr(), conversationId, operatorUserId);
            AssertUtils.isTrue(memberResponse.getActiveMember(), BizErrorEnum.NOT_CONVERSATION_MEMBER);
            return ConversationAssetAccess.builder()
                    .conversationId(conversationId)
                    .conversationType(ConversationTypeEnum.GROUP)
                    .groupId(groupId)
                    .operatorUserId(operatorUserId)
                    .groupRole(GroupMemberRoleEnum.valueOf(memberResponse.getRole()))
                    .activeMember(Boolean.TRUE)
                    .build();
        } catch (BizException e) {
            if (BizErrorEnum.NOT_CONVERSATION_MEMBER.equals(e.getError())
                    || BizErrorEnum.ASSET_ACCESS_CONTEXT_QUERY_FAILED.equals(e.getError())) {
                throw e;
            }
            log.error(
                    "查询会话资产访问上下文业务失败, conversationId={}, operatorUserId={}, code={}",
                    conversationId,
                    operatorUserId,
                    e.getError().getErrCode(),
                    e
            );
            throw new BizException(BizErrorEnum.ASSET_ACCESS_CONTEXT_QUERY_FAILED);
        } catch (Exception e) {
            log.error(
                    "查询会话资产访问上下文系统失败, conversationId={}, operatorUserId={}",
                    conversationId,
                    operatorUserId,
                    e
            );
            throw new BizException(BizErrorEnum.ASSET_ACCESS_CONTEXT_QUERY_FAILED);
        }
    }

    /**
     * 批量查询当前用户名映射.
     *
     * @param userIdList 用户id列表
     * @return 用户名映射
     */
    public Map<BigInteger, String> getUsernameMap(final List<BigInteger> userIdList) {
        if (Objects.isNull(userIdList) || userIdList.isEmpty()) {
            return Map.of();
        }
        List<BigInteger> distinctUserIdList = userIdList.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctUserIdList.isEmpty()) {
            return Map.of();
        }
        try {
            ConversationTargetInfoResponse response = userGrpcServiceBlockingStub.getConversationTargetInfo(
                    ConversationTargetInfoRequest.newBuilder()
                            .addAllUserId(distinctUserIdList.stream().map(BigInteger::toString).toList())
                            .build()
            );
            if (!response.getBr().getSuccess()) {
                log.warn(
                        "批量查询资产目录用户名失败, userIdList={}, code={}, message={}",
                        distinctUserIdList,
                        response.getBr().getCode(),
                        response.getBr().getMessage()
                );
                throw new BizException(BizErrorEnum.ASSET_ACCESS_CONTEXT_QUERY_FAILED);
            }
            return response.getUserInfoList().stream()
                    .collect(Collectors.toMap(
                            userInfo -> new BigInteger(userInfo.getUserId()),
                            ConversationUserInfo::getUsername,
                            (oldValue, newValue) -> oldValue
                    ));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量查询资产目录用户名系统失败, userIdList={}", distinctUserIdList, e);
            throw new BizException(BizErrorEnum.ASSET_ACCESS_CONTEXT_QUERY_FAILED);
        }
    }

    /**
     * 校验远程基础响应.
     *
     * @param baseResponse    基础响应
     * @param conversationId 会话id
     * @param operatorUserId 操作用户id
     */
    private void validateBaseResponse(
            final BaseResponse baseResponse,
            final BigInteger conversationId,
            final BigInteger operatorUserId
    ) {
        if (!baseResponse.getSuccess()) {
            log.warn(
                    "查询会话资产访问上下文远程失败, conversationId={}, operatorUserId={}, code={}, message={}",
                    conversationId,
                    operatorUserId,
                    baseResponse.getCode(),
                    baseResponse.getMessage()
            );
            throw new BizException(BizErrorEnum.ASSET_ACCESS_CONTEXT_QUERY_FAILED);
        }
    }
}
