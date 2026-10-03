package com.devin.uniontalk.message.grpc;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.exception.BizException;
import com.devin.uniontalk.grpc.base.BaseResponse;
import com.devin.uniontalk.grpc.message.domain.request.CreateAgentReplyRequest;
import com.devin.uniontalk.grpc.message.domain.request.GetAgentConversationContextRequest;
import com.devin.uniontalk.grpc.message.domain.request.GetAgentConversationPermissionRequest;
import com.devin.uniontalk.grpc.message.domain.response.CreateAgentReplyResponse;
import com.devin.uniontalk.grpc.message.domain.response.GetAgentConversationContextResponse;
import com.devin.uniontalk.grpc.message.domain.response.GetAgentConversationPermissionResponse;
import com.devin.uniontalk.grpc.message.service.MessageGrpcServiceGrpc;
import com.devin.uniontalk.infrastructure.message.enums.AgentCitationSourceTypeEnum;
import com.devin.uniontalk.message.domain.command.AgentReplyCommand;
import com.devin.uniontalk.message.domain.model.AgentCitation;
import com.devin.uniontalk.message.domain.model.AgentConversationContext;
import com.devin.uniontalk.message.domain.model.AgentConversationMessage;
import com.devin.uniontalk.message.domain.model.AgentConversationPermission;
import com.devin.uniontalk.message.domain.model.AgentReplyResult;
import com.devin.uniontalk.message.service.AgentConversationQueryService;
import com.devin.uniontalk.message.service.AgentReplyCommandService;
import com.devin.uniontalk.web.response.ResultEnum;
import io.grpc.stub.StreamObserver;
import java.math.BigInteger;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.util.StringUtils;

/**
 * 2026/07/28 23:35.
 *
 * <p>
 * 消息Grpc服务实现
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class MessageGrpcServiceImpl extends MessageGrpcServiceGrpc.MessageGrpcServiceImplBase {

    /**
     * Agent回复写回命令服务.
     */
    private final AgentReplyCommandService agentReplyCommandService;

    /**
     * Agent会话查询服务.
     */
    private final AgentConversationQueryService agentConversationQueryService;

    /**
     * 创建Agent正式回复.
     *
     * @param request          创建Agent回复请求
     * @param responseObserver 创建Agent回复响应观察器
     */
    @Override
    public void createAgentReply(
            final CreateAgentReplyRequest request,
            final StreamObserver<CreateAgentReplyResponse> responseObserver
    ) {
        try {
            AgentReplyResult result = agentReplyCommandService.createAgentReply(toCommand(request));
            responseObserver.onNext(successResponse(result));
        } catch (BizException e) {
            responseObserver.onNext(failureResponse(
                    e.getError().getErrCode(),
                    e.getError().getErrMsg()
            ));
        } catch (Exception e) {
            log.error(
                    "创建Agent正式回复系统失败, runId={}, triggerMessageId={}",
                    request.getRunId(),
                    request.getTriggerMessageId(),
                    e
            );
            responseObserver.onNext(failureResponse(
                    ResultEnum.SYSTEM_ERROR.getCode(),
                    ResultEnum.SYSTEM_ERROR.getMessage()
            ));
        } finally {
            responseObserver.onCompleted();
        }
    }

    /**
     * 查询Agent回答使用的紧凑会话上下文.
     *
     * @param request          Agent会话上下文请求
     * @param responseObserver Agent会话上下文响应观察器
     */
    @Override
    public void getAgentConversationContext(
            final GetAgentConversationContextRequest request,
            final StreamObserver<GetAgentConversationContextResponse> responseObserver
    ) {
        try {
            AgentConversationContext context = agentConversationQueryService.getContext(
                    parseRequiredId(request.getConversationId()),
                    parseRequiredId(request.getTriggerMessageId()),
                    request.getRecentMessageLimit()
            );
            responseObserver.onNext(contextSuccessResponse(context));
        } catch (BizException e) {
            responseObserver.onNext(contextFailureResponse(
                    e.getError().getErrCode(),
                    e.getError().getErrMsg()
            ));
        } catch (Exception e) {
            log.error(
                    "查询Agent会话上下文系统失败, conversationId={}, triggerMessageId={}",
                    request.getConversationId(),
                    request.getTriggerMessageId(),
                    e
            );
            responseObserver.onNext(contextFailureResponse(
                    ResultEnum.SYSTEM_ERROR.getCode(),
                    ResultEnum.SYSTEM_ERROR.getMessage()
            ));
        } finally {
            responseObserver.onCompleted();
        }
    }

    /**
     * 查询当前用户的会话Agent权限.
     *
     * @param request          会话Agent权限请求
     * @param responseObserver 会话Agent权限响应观察器
     */
    @Override
    public void getAgentConversationPermission(
            final GetAgentConversationPermissionRequest request,
            final StreamObserver<GetAgentConversationPermissionResponse> responseObserver
    ) {
        try {
            AgentConversationPermission permission = agentConversationQueryService.getPermission(
                    parseRequiredId(request.getConversationId()),
                    parseRequiredId(request.getUserId())
            );
            responseObserver.onNext(permissionSuccessResponse(permission));
        } catch (BizException e) {
            responseObserver.onNext(permissionFailureResponse(
                    e.getError().getErrCode(),
                    e.getError().getErrMsg()
            ));
        } catch (Exception e) {
            log.error(
                    "查询会话Agent权限系统失败, conversationId={}, userId={}",
                    request.getConversationId(),
                    request.getUserId(),
                    e
            );
            responseObserver.onNext(permissionFailureResponse(
                    ResultEnum.SYSTEM_ERROR.getCode(),
                    ResultEnum.SYSTEM_ERROR.getMessage()
            ));
        } finally {
            responseObserver.onCompleted();
        }
    }

    /**
     * 转换Agent回复写回命令.
     *
     * @param request 创建Agent回复请求
     * @return Agent回复写回命令
     */
    private AgentReplyCommand toCommand(final CreateAgentReplyRequest request) {
        return AgentReplyCommand.builder()
                .runId(parseRequiredId(request.getRunId()))
                .conversationId(parseRequiredId(request.getConversationId()))
                .triggerMessageId(parseRequiredId(request.getTriggerMessageId()))
                .replyToUserId(parseRequiredId(request.getReplyToUserId()))
                .agentId(parseRequiredId(request.getAgentId()))
                .modelId(request.getModelId().trim())
                .content(request.getContent())
                .citationList(request.getCitationsList()
                        .stream()
                        .map(this::toCitation)
                        .toList())
                .idempotencyKey(request.getIdempotencyKey())
                .build();
    }

    /**
     * 转换Agent引用信息.
     *
     * @param citation Agent引用Proto
     * @return Agent引用信息
     */
    private AgentCitation toCitation(
            final com.devin.uniontalk.grpc.message.domain.model.AgentCitation citation
    ) {
        return AgentCitation.builder()
                .citationKey(citation.getCitationKey().trim())
                .sourceType(parseCitationSourceType(citation.getSourceType()))
                .messageId(parseOptionalId(citation.getMessageId()))
                .assetFileId(parseOptionalId(citation.getAssetFileId()))
                .resourceVersion(toOptionalInteger(citation.getResourceVersion()))
                .chunkId(parseOptionalId(citation.getChunkId()))
                .pageFrom(toOptionalInteger(citation.getPageFrom()))
                .pageTo(toOptionalInteger(citation.getPageTo()))
                .headingPath(citation.getHeadingPath())
                .build();
    }

    /**
     * 解析必填id.
     *
     * @param id id字符串
     * @return id
     */
    private BigInteger parseRequiredId(final String id) {
        if (!StringUtils.hasText(id)) {
            throw new BizException(BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        }
        try {
            BigInteger parsedId = new BigInteger(id);
            if (parsedId.signum() <= 0
                    || parsedId.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                throw new BizException(BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
            }
            return parsedId;
        } catch (NumberFormatException e) {
            throw new BizException(BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        }
    }

    /**
     * 解析Agent引用来源类型.
     *
     * @param sourceType 引用来源类型编码
     * @return Agent引用来源类型
     */
    private AgentCitationSourceTypeEnum parseCitationSourceType(final String sourceType) {
        if (!AgentCitationSourceTypeEnum.isSupported(sourceType)) {
            throw new BizException(BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        }
        return AgentCitationSourceTypeEnum.valueOf(sourceType);
    }

    /**
     * 解析可选id.
     *
     * @param id id字符串
     * @return id，未传返回null
     */
    private BigInteger parseOptionalId(final String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        try {
            BigInteger parsedId = new BigInteger(id);
            if (parsedId.signum() <= 0
                    || parsedId.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                throw new BizException(BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
            }
            return parsedId;
        } catch (NumberFormatException e) {
            throw new BizException(BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        }
    }

    /**
     * 将Proto默认零值转换为可选整数.
     *
     * @param value Proto整数值
     * @return 正数值，非正数返回null
     */
    private Integer toOptionalInteger(final Integer value) {
        if (value < 0) {
            throw new BizException(BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        }
        return value > 0 ? value : null;
    }

    /**
     * 构建成功响应.
     *
     * @param result Agent回复写回结果
     * @return 创建Agent回复响应
     */
    private CreateAgentReplyResponse successResponse(final AgentReplyResult result) {
        return CreateAgentReplyResponse.newBuilder()
                .setBr(successBaseResponse())
                .setAnswerMessageId(result.getAnswerMessageId().toString())
                .setCreated(Boolean.TRUE.equals(result.getCreated()))
                .build();
    }

    /**
     * 构建Agent会话上下文成功响应.
     *
     * @param context Agent紧凑会话上下文
     * @return Agent会话上下文响应
     */
    private GetAgentConversationContextResponse contextSuccessResponse(
            final AgentConversationContext context
    ) {
        GetAgentConversationContextResponse.Builder responseBuilder =
                GetAgentConversationContextResponse.newBuilder()
                .setBr(successBaseResponse())
                .setConversationId(context.getConversationId().toString())
                .setTriggerMessageId(context.getTriggerMessageId().toString())
                .setQuestion(context.getQuestion())
                .addAllRecentMessages(context.getRecentMessageList()
                        .stream()
                        .map(this::toContextMessageProto)
                        .toList())
                .addAllReceiverUserIds(context.getReceiverUserIdList()
                        .stream()
                        .map(BigInteger::toString)
                        .toList())
                .addAllReferencedResourceIds(context.getReferencedResourceIdList()
                        .stream()
                        .map(BigInteger::toString)
                        .toList())
                .addAllReferencedAssetFileIds(context.getReferencedAssetFileIdList()
                        .stream()
                        .map(BigInteger::toString)
                        .toList());
        if (Objects.nonNull(context.getQuotedMessageId())) {
            responseBuilder.setQuotedMessageId(context.getQuotedMessageId().toString());
        }
        return responseBuilder.build();
    }

    /**
     * 转换Agent紧凑会话消息.
     *
     * @param message Agent紧凑会话消息
     * @return Agent会话消息Proto
     */
    private com.devin.uniontalk.grpc.message.domain.model.AgentConversationMessage toContextMessageProto(
            final AgentConversationMessage message
    ) {
        return com.devin.uniontalk.grpc.message.domain.model.AgentConversationMessage.newBuilder()
                .setMessageId(message.getMessageId().toString())
                .setSenderType(message.getSenderType().name())
                .setSenderDisplayName(message.getSenderDisplayName())
                .setContent(message.getContent())
                .setCreatedAtMs(message.getCreatedAt().getTime())
                .build();
    }

    /**
     * 构建会话Agent权限成功响应.
     *
     * @param permission 当前会话Agent权限
     * @return 会话Agent权限响应
     */
    private GetAgentConversationPermissionResponse permissionSuccessResponse(
            final AgentConversationPermission permission
    ) {
        return GetAgentConversationPermissionResponse.newBuilder()
                .setBr(successBaseResponse())
                .setMember(Boolean.TRUE.equals(permission.getMember()))
                .setRole(permission.getRole() == null ? "" : permission.getRole().name())
                .setConversationType(
                        permission.getConversationType() == null
                                ? ""
                                : permission.getConversationType().name()
                )
                .setGroupId(
                        permission.getGroupId() == null
                                ? ""
                                : permission.getGroupId().toString()
                )
                .build();
    }

    /**
     * 构建失败响应.
     *
     * @param code    错误码
     * @param message 错误信息
     * @return 创建Agent回复响应
     */
    private CreateAgentReplyResponse failureResponse(final Integer code, final String message) {
        return CreateAgentReplyResponse.newBuilder()
                .setBr(failureBaseResponse(code, message))
                .build();
    }

    /**
     * 构建Agent会话上下文失败响应.
     *
     * @param code    状态码
     * @param message 状态信息
     * @return Agent会话上下文响应
     */
    private GetAgentConversationContextResponse contextFailureResponse(
            final Integer code,
            final String message
    ) {
        return GetAgentConversationContextResponse.newBuilder()
                .setBr(failureBaseResponse(code, message))
                .build();
    }

    /**
     * 构建会话Agent权限失败响应.
     *
     * @param code    状态码
     * @param message 状态信息
     * @return 会话Agent权限响应
     */
    private GetAgentConversationPermissionResponse permissionFailureResponse(
            final Integer code,
            final String message
    ) {
        return GetAgentConversationPermissionResponse.newBuilder()
                .setBr(failureBaseResponse(code, message))
                .setMember(Boolean.FALSE)
                .setRole("")
                .setConversationType("")
                .setGroupId("")
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

    /**
     * 构建失败基础响应.
     *
     * @param code    状态码
     * @param message 状态信息
     * @return 失败基础响应
     */
    private BaseResponse failureBaseResponse(final Integer code, final String message) {
        return BaseResponse.newBuilder()
                .setSuccess(Boolean.FALSE)
                .setCode(code)
                .setMessage(message)
                .build();
    }
}
