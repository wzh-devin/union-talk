package com.devin.uniontalk.message.grpc;

import com.devin.uniontalk.grpc.base.BaseResponse;
import com.devin.uniontalk.grpc.message.domain.request.CheckConversationMemberRequest;
import com.devin.uniontalk.grpc.message.domain.request.CreateGroupConversationRequest;
import com.devin.uniontalk.grpc.message.domain.request.CreatePrivateConversationRequest;
import com.devin.uniontalk.grpc.message.domain.request.DissolveGroupConversationRequest;
import com.devin.uniontalk.grpc.message.domain.request.GetConversationAssetContextRequest;
import com.devin.uniontalk.grpc.message.domain.response.CheckConversationMemberResponse;
import com.devin.uniontalk.grpc.message.domain.response.CreateGroupConversationResponse;
import com.devin.uniontalk.grpc.message.domain.response.CreatePrivateConversationResponse;
import com.devin.uniontalk.grpc.message.domain.response.DissolveGroupConversationResponse;
import com.devin.uniontalk.grpc.message.domain.response.GetConversationAssetContextResponse;
import com.devin.uniontalk.grpc.message.service.ConversationGrpcServiceGrpc;
import com.devin.uniontalk.message.domain.model.ConversationAssetContext;
import com.devin.uniontalk.message.service.ConversationService;
import com.devin.uniontalk.web.response.ResultEnum;
import io.grpc.stub.StreamObserver;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * 2026/05/20 18:30.
 *
 * <p>
 * 会话Grpc服务实现类
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ConversationGrpcServiceImpl extends ConversationGrpcServiceGrpc.ConversationGrpcServiceImplBase {

    /**
     * 会话服务.
     */
    private final ConversationService conversationService;

    /**
     * 创建私聊会话.
     *
     * @param request          创建私聊会话请求
     * @param responseObserver 创建私聊会话响应观察器
     */
    @Override
    public void createPrivateConversation(
            final CreatePrivateConversationRequest request,
            final StreamObserver<CreatePrivateConversationResponse> responseObserver
    ) {
        try {
            BigInteger fromUserId = parseId(request.getFromUserId());
            BigInteger toUserId = parseId(request.getToUserId());
            BigInteger conversationId = conversationService.createPrivateConversation(fromUserId, toUserId);
            responseObserver.onNext(successResponse(conversationId));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("创建私聊会话系统失败, friendRequestId={}", request.getFriendRequestId(), e);
            responseObserver.onNext(failureResponse(ResultEnum.SYSTEM_ERROR.getCode(), e.getMessage()));
            responseObserver.onCompleted();
        }
    }

    /**
     * 创建群聊会话.
     *
     * @param request          创建群聊会话请求
     * @param responseObserver 创建群聊会话响应观察器
     */
    @Override
    public void createGroupConversation(
            final CreateGroupConversationRequest request,
            final StreamObserver<CreateGroupConversationResponse> responseObserver
    ) {
        try {
            BigInteger groupId = parseId(request.getGroupId());
            List<BigInteger> memberIdList = request.getMemberIdList().stream()
                    .map(this::parseId)
                    .toList();
            BigInteger conversationId = conversationService.createGroupConversation(groupId, memberIdList);
            responseObserver.onNext(successGroupResponse(conversationId));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("创建群聊会话系统失败, groupId={}", request.getGroupId(), e);
            responseObserver.onNext(groupFailureResponse(ResultEnum.SYSTEM_ERROR.getCode(), e.getMessage()));
            responseObserver.onCompleted();
        }
    }

    /**
     * 解散群聊关联会话.
     *
     * @param request          解散群聊关联会话请求
     * @param responseObserver 解散群聊关联会话响应观察器
     */
    @Override
    public void dissolveGroupConversation(
            final DissolveGroupConversationRequest request,
            final StreamObserver<DissolveGroupConversationResponse> responseObserver
    ) {
        try {
            BigInteger groupId = parseId(request.getGroupId());
            conversationService.dissolveGroupConversation(groupId);
            responseObserver.onNext(dissolveSuccessResponse());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("解散群聊关联会话系统失败, groupId={}", request.getGroupId(), e);
            responseObserver.onNext(dissolveFailureResponse(ResultEnum.SYSTEM_ERROR.getCode(), e.getMessage()));
            responseObserver.onCompleted();
        }
    }

    /**
     * 校验用户是否属于会话.
     *
     * @param request          校验会话成员请求
     * @param responseObserver 校验会话成员响应观察器
     */
    @Override
    public void checkConversationMember(
            final CheckConversationMemberRequest request,
            final StreamObserver<CheckConversationMemberResponse> responseObserver
    ) {
        try {
            BigInteger conversationId = parseId(request.getConversationId());
            BigInteger userId = parseId(request.getUserId());
            boolean exists = conversationService.existsConversationMember(conversationId, userId);
            responseObserver.onNext(checkSuccessResponse(exists));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(
                    "校验会话成员系统失败, conversationId={}, userId={}",
                    request.getConversationId(),
                    request.getUserId(),
                    e
            );
            responseObserver.onNext(checkFailureResponse(ResultEnum.SYSTEM_ERROR.getCode(), e.getMessage()));
            responseObserver.onCompleted();
        }
    }

    /**
     * 查询会话资产上下文.
     *
     * @param request          会话资产上下文请求
     * @param responseObserver 会话资产上下文响应观察器
     */
    @Override
    public void getConversationAssetContext(
            final GetConversationAssetContextRequest request,
            final StreamObserver<GetConversationAssetContextResponse> responseObserver
    ) {
        try {
            BigInteger conversationId = parseId(request.getConversationId());
            BigInteger operatorUserId = parseId(request.getOperatorUserId());
            ConversationAssetContext context = conversationService.getConversationAssetContext(
                    conversationId,
                    operatorUserId
            );
            responseObserver.onNext(GetConversationAssetContextResponse.newBuilder()
                    .setBr(successBaseResponse())
                    .setConversationType(context.getConversationType().name())
                    .setGroupId(Objects.toString(context.getGroupId(), ""))
                    .setPrivateMember(Boolean.TRUE.equals(context.getPrivateMember()))
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(
                    "查询会话资产上下文系统失败, conversationId={}, operatorUserId={}",
                    request.getConversationId(),
                    request.getOperatorUserId(),
                    e
            );
            responseObserver.onNext(GetConversationAssetContextResponse.newBuilder()
                    .setBr(BaseResponse.newBuilder()
                            .setSuccess(Boolean.FALSE)
                            .setCode(ResultEnum.SYSTEM_ERROR.getCode())
                            .setMessage(ResultEnum.SYSTEM_ERROR.getMessage())
                            .build())
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 解析id.
     *
     * @param id id字符串
     * @return id
     */
    private BigInteger parseId(final String id) {
        return new BigInteger(id);
    }

    /**
     * 构建成功响应.
     *
     * @param conversationId 会话id
     * @return 创建私聊会话响应
     */
    private CreatePrivateConversationResponse successResponse(final BigInteger conversationId) {
        return CreatePrivateConversationResponse.newBuilder()
                .setBr(successBaseResponse())
                .setConversationId(conversationId.toString())
                .build();
    }

    /**
     * 构建群聊会话成功响应.
     *
     * @param conversationId 会话id
     * @return 创建群聊会话响应
     */
    private CreateGroupConversationResponse successGroupResponse(final BigInteger conversationId) {
        return CreateGroupConversationResponse.newBuilder()
                .setBr(successBaseResponse())
                .setConversationId(conversationId.toString())
                .build();
    }

    /**
     * 构建失败响应.
     *
     * @param code    状态码
     * @param message 状态信息
     * @return 创建私聊会话响应
     */
    private CreatePrivateConversationResponse failureResponse(final Integer code, final String message) {
        return CreatePrivateConversationResponse.newBuilder()
                .setBr(BaseResponse.newBuilder()
                        .setSuccess(Boolean.FALSE)
                        .setCode(code)
                        .setMessage(message)
                        .build())
                .build();
    }

    /**
     * 构建群聊会话失败响应.
     *
     * @param code    状态码
     * @param message 状态信息
     * @return 创建群聊会话响应
     */
    private CreateGroupConversationResponse groupFailureResponse(final Integer code, final String message) {
        return CreateGroupConversationResponse.newBuilder()
                .setBr(BaseResponse.newBuilder()
                        .setSuccess(Boolean.FALSE)
                        .setCode(code)
                        .setMessage(message)
                        .build())
                .build();
    }

    /**
     * 构建解散群聊关联会话成功响应.
     *
     * @return 解散群聊关联会话响应
     */
    private DissolveGroupConversationResponse dissolveSuccessResponse() {
        return DissolveGroupConversationResponse.newBuilder()
                .setBr(successBaseResponse())
                .build();
    }

    /**
     * 构建解散群聊关联会话失败响应.
     *
     * @param code    状态码
     * @param message 状态信息
     * @return 解散群聊关联会话响应
     */
    private DissolveGroupConversationResponse dissolveFailureResponse(final Integer code, final String message) {
        return DissolveGroupConversationResponse.newBuilder()
                .setBr(BaseResponse.newBuilder()
                        .setSuccess(Boolean.FALSE)
                        .setCode(code)
                        .setMessage(message)
                        .build())
                .build();
    }

    /**
     * 构建会话成员校验成功响应.
     *
     * @param exists 是否属于会话
     * @return 会话成员校验响应
     */
    private CheckConversationMemberResponse checkSuccessResponse(final boolean exists) {
        return CheckConversationMemberResponse.newBuilder()
                .setBr(successBaseResponse())
                .setExists(exists)
                .build();
    }

    /**
     * 构建会话成员校验失败响应.
     *
     * @param code    状态码
     * @param message 状态信息
     * @return 会话成员校验响应
     */
    private CheckConversationMemberResponse checkFailureResponse(final Integer code, final String message) {
        return CheckConversationMemberResponse.newBuilder()
                .setBr(BaseResponse.newBuilder()
                        .setSuccess(Boolean.FALSE)
                        .setCode(code)
                        .setMessage(message)
                        .build())
                .setExists(Boolean.FALSE)
                .build();
    }

    /**
     * 构建成功基础响应.
     *
     * @return 基础响应
     */
    private BaseResponse successBaseResponse() {
        return BaseResponse.newBuilder()
                .setSuccess(Boolean.TRUE)
                .setCode(ResultEnum.SUCCESS.getCode())
                .setMessage(ResultEnum.SUCCESS.getMessage())
                .build();
    }
}
