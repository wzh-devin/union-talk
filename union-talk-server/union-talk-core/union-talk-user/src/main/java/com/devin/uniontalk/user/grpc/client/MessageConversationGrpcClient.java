package com.devin.uniontalk.user.grpc.client;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.exception.BizException;
import com.devin.uniontalk.grpc.message.domain.request.CreateGroupConversationRequest;
import com.devin.uniontalk.grpc.message.domain.request.CreatePrivateConversationRequest;
import com.devin.uniontalk.grpc.message.domain.request.DissolveGroupConversationRequest;
import com.devin.uniontalk.grpc.message.domain.response.CreateGroupConversationResponse;
import com.devin.uniontalk.grpc.message.domain.response.CreatePrivateConversationResponse;
import com.devin.uniontalk.grpc.message.domain.response.DissolveGroupConversationResponse;
import com.devin.uniontalk.grpc.message.service.ConversationGrpcServiceGrpc;
import java.math.BigInteger;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * 2026/05/20 18:30.
 *
 * <p>
 * Message会话Grpc客户端
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
public class MessageConversationGrpcClient {

    /**
     * 会话Grpc阻塞调用客户端.
     */
    @GrpcClient("union-talk-message")
    private ConversationGrpcServiceGrpc.ConversationGrpcServiceBlockingStub conversationGrpcServiceBlockingStub;

    /**
     * 创建私聊会话.
     *
     * @param fromUserId      申请用户id
     * @param toUserId        接受用户id
     * @param friendRequestId 好友申请id
     * @return 会话id
     */
    public BigInteger createPrivateConversation(
            final BigInteger fromUserId,
            final BigInteger toUserId,
            final BigInteger friendRequestId
    ) {
        try {
            CreatePrivateConversationResponse response = conversationGrpcServiceBlockingStub.createPrivateConversation(
                    CreatePrivateConversationRequest.newBuilder()
                            .setFromUserId(fromUserId.toString())
                            .setToUserId(toUserId.toString())
                            .setFriendRequestId(friendRequestId.toString())
                            .build()
            );
            validateResponse(response, friendRequestId);
            return new BigInteger(response.getConversationId());
        } catch (Exception e) {
            log.error("调用Message创建私聊会话Grpc失败, friendRequestId={}", friendRequestId, e);
            throw new BizException(BizErrorEnum.CONVERSATION_CREATE_FAILED);
        }
    }

    /**
     * 创建群聊会话.
     *
     * @param groupId      群聊id
     * @param memberIdList 群成员id列表
     * @return 会话id
     */
    public BigInteger createGroupConversation(final BigInteger groupId, final List<BigInteger> memberIdList) {
        try {
            CreateGroupConversationResponse response = conversationGrpcServiceBlockingStub.createGroupConversation(
                    CreateGroupConversationRequest.newBuilder()
                            .setGroupId(groupId.toString())
                            .addAllMemberId(memberIdList.stream()
                                    .map(BigInteger::toString)
                                    .toList())
                            .build()
            );
            validateGroupResponse(response, groupId);
            return new BigInteger(response.getConversationId());
        } catch (Exception e) {
            log.error("调用Message创建群聊会话Grpc失败, groupId={}", groupId, e);
            throw new BizException(BizErrorEnum.CONVERSATION_CREATE_FAILED);
        }
    }

    /**
     * 解散群聊关联会话.
     *
     * @param groupId 群聊id
     */
    public void dissolveGroupConversation(final BigInteger groupId) {
        try {
            DissolveGroupConversationResponse response =
                    conversationGrpcServiceBlockingStub.dissolveGroupConversation(
                            DissolveGroupConversationRequest.newBuilder()
                                    .setGroupId(groupId.toString())
                                    .build()
                    );
            validateDissolveResponse(response, groupId);
        } catch (Exception e) {
            log.error("调用Message解散群聊关联会话Grpc失败, groupId={}", groupId, e);
            throw new BizException(BizErrorEnum.CONVERSATION_DISSOLVE_FAILED);
        }
    }

    /**
     * 校验创建私聊会话响应.
     *
     * @param response        创建私聊会话响应
     * @param friendRequestId 好友申请id
     */
    private void validateResponse(
            final CreatePrivateConversationResponse response,
            final BigInteger friendRequestId
    ) {
        if (!response.getBr().getSuccess()) {
            log.warn(
                    "Message创建私聊会话失败, friendRequestId={}, code={}, message={}",
                    friendRequestId,
                    response.getBr().getCode(),
                    response.getBr().getMessage()
            );
            throw new BizException(BizErrorEnum.CONVERSATION_CREATE_FAILED);
        }
    }

    /**
     * 校验创建群聊会话响应.
     *
     * @param response 创建群聊会话响应
     * @param groupId  群聊id
     */
    private void validateGroupResponse(
            final CreateGroupConversationResponse response,
            final BigInteger groupId
    ) {
        if (!response.getBr().getSuccess()) {
            log.warn(
                    "Message创建群聊会话失败, groupId={}, code={}, message={}",
                    groupId,
                    response.getBr().getCode(),
                    response.getBr().getMessage()
            );
            throw new BizException(BizErrorEnum.CONVERSATION_CREATE_FAILED);
        }
    }

    /**
     * 校验解散群聊关联会话响应.
     *
     * @param response 解散群聊关联会话响应
     * @param groupId  群聊id
     */
    private void validateDissolveResponse(
            final DissolveGroupConversationResponse response,
            final BigInteger groupId
    ) {
        if (!response.getBr().getSuccess()) {
            log.warn(
                    "Message解散群聊关联会话失败, groupId={}, code={}, message={}",
                    groupId,
                    response.getBr().getCode(),
                    response.getBr().getMessage()
            );
            throw new BizException(BizErrorEnum.CONVERSATION_DISSOLVE_FAILED);
        }
    }
}
