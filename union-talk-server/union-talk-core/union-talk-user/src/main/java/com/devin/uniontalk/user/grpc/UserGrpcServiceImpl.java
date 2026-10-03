package com.devin.uniontalk.user.grpc;

import com.devin.uniontalk.base.exception.BizException;
import com.devin.uniontalk.grpc.base.BaseResponse;
import com.devin.uniontalk.grpc.user.domain.model.ConversationGroupInfo;
import com.devin.uniontalk.grpc.user.domain.model.ConversationUserInfo;
import com.devin.uniontalk.grpc.user.domain.request.CheckUserExistRequest;
import com.devin.uniontalk.grpc.user.domain.request.ConversationTargetInfoRequest;
import com.devin.uniontalk.grpc.user.domain.request.GetGroupAssetMemberContextRequest;
import com.devin.uniontalk.grpc.user.domain.request.LoginRequest;
import com.devin.uniontalk.grpc.user.domain.request.RegisterRequest;
import com.devin.uniontalk.grpc.user.domain.response.CheckUserExistResponse;
import com.devin.uniontalk.grpc.user.domain.response.ConversationTargetInfoResponse;
import com.devin.uniontalk.grpc.user.domain.response.GetGroupAssetMemberContextResponse;
import com.devin.uniontalk.grpc.user.domain.response.LoginResponse;
import com.devin.uniontalk.grpc.user.domain.response.RegisterResponse;
import com.devin.uniontalk.grpc.user.service.UserGrpcServiceGrpc;
import com.devin.uniontalk.infrastructure.user.enums.DeviceTypeEnum;
import com.devin.uniontalk.user.domain.entity.User;
import com.devin.uniontalk.user.domain.entity.convertor.UserConvertor;
import com.devin.uniontalk.user.domain.model.ConversationTargetInfo;
import com.devin.uniontalk.user.domain.vo.resp.GroupMemberRespVO;
import com.devin.uniontalk.user.service.ConversationTargetInfoService;
import com.devin.uniontalk.user.service.GroupMemberService;
import com.devin.uniontalk.user.service.UserService;
import com.devin.uniontalk.web.response.ResultEnum;
import io.grpc.stub.StreamObserver;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * 2026/5/13 00:14.
 *
 * <p>
 * 用户Grpc服务实现类
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcServiceImpl extends UserGrpcServiceGrpc.UserGrpcServiceImplBase {

    /**
     * 用户服务.
     */
    private final UserService userService;

    /**
     * 会话目标信息服务.
     */
    private final ConversationTargetInfoService conversationTargetInfoService;

    /**
     * 群成员服务.
     */
    private final GroupMemberService groupMemberService;

    /**
     * 校验用户是否存在.
     *
     * @param request          校验用户是否存在请求
     * @param responseObserver 校验用户是否存在响应观察器
     */
    @Override
    public void checkUserExist(
            final CheckUserExistRequest request,
            final StreamObserver<CheckUserExistResponse> responseObserver
    ) {
        Boolean exist = userService.checkUserExist(request.getUsername(), request.getEmail());
        CheckUserExistResponse response = CheckUserExistResponse.newBuilder()
                .setBr(successBaseResponse())
                .setExist(exist)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * 注册.
     *
     * @param request          注册请求
     * @param responseObserver 注册响应观察器
     */
    @Override
    public void register(
            final RegisterRequest request,
            final StreamObserver<RegisterResponse> responseObserver
    ) {
        User user = userService.register(request.getUsername(), request.getEmail(), request.getPassword());
        RegisterResponse registerResponse = RegisterResponse.newBuilder()
                .setBr(successBaseResponse())
                .setUserInfo(UserConvertor.INSTANCE.toUserInfoProto(user))
                .build();
        responseObserver.onNext(registerResponse);
        responseObserver.onCompleted();
    }

    /**
     * 构建成功响应.
     *
     * @return 响应
     */
    private BaseResponse successBaseResponse() {
        return BaseResponse.newBuilder()
                .setSuccess(Boolean.TRUE)
                .setCode(ResultEnum.SUCCESS.getCode())
                .setMessage(ResultEnum.SUCCESS.getMessage())
                .build();
    }

    /**
     * 登录.
     *
     * @param request          登录请求
     * @param responseObserver 登录响应观察器
     */
    @Override
    public void login(
            final LoginRequest request,
            final StreamObserver<LoginResponse> responseObserver
    ) {
        try {
            User user = userService.login(
                    request.getAccount(),
                    request.getPassword(),
                    request.getIpAddress(),
                    request.getUserAgent(),
                    DeviceTypeEnum.of(request.getDeviceType()),
                    request.getDeviceName()
            );
            LoginResponse loginResponse = LoginResponse.newBuilder()
                    .setBr(successBaseResponse())
                    .setUserInfo(UserConvertor.INSTANCE.toUserInfoProto(user))
                    .build();
            responseObserver.onNext(loginResponse);
            responseObserver.onCompleted();
        } catch (BizException e) {
            LoginResponse loginResponse = LoginResponse.newBuilder()
                    .setBr(BaseResponse.newBuilder()
                            .setSuccess(Boolean.FALSE)
                            .setCode(e.getError().getErrCode())
                            .setMessage(e.getError().getErrMsg())
                            .build())
                    .build();
            responseObserver.onNext(loginResponse);
            responseObserver.onCompleted();
        }
    }

    /**
     * 查询会话目标信息.
     *
     * @param request          会话目标信息请求
     * @param responseObserver 会话目标信息响应观察器
     */
    @Override
    public void getConversationTargetInfo(
            final ConversationTargetInfoRequest request,
            final StreamObserver<ConversationTargetInfoResponse> responseObserver
    ) {
        try {
            ConversationTargetInfo targetInfo = conversationTargetInfoService.getConversationTargetInfo(
                    getIdList(request.getUserIdList()),
                    getIdList(request.getGroupIdList())
            );
            responseObserver.onNext(successConversationTargetInfoResponse(targetInfo));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(
                    "查询会话目标信息Grpc失败, userIdList={}, groupIdList={}",
                    request.getUserIdList(),
                    request.getGroupIdList(),
                    e
            );
            responseObserver.onNext(conversationTargetInfoFailureResponse(
                    ResultEnum.SYSTEM_ERROR.getCode(),
                    e.getMessage()
            ));
            responseObserver.onCompleted();
        }
    }

    /**
     * 查询群成员资产上下文.
     *
     * @param request          群成员资产上下文请求
     * @param responseObserver 群成员资产上下文响应观察器
     */
    @Override
    public void getGroupAssetMemberContext(
            final GetGroupAssetMemberContextRequest request,
            final StreamObserver<GetGroupAssetMemberContextResponse> responseObserver
    ) {
        try {
            GroupMemberRespVO memberContext = groupMemberService.getGroupAssetMemberContext(
                    new BigInteger(request.getGroupId()),
                    new BigInteger(request.getOperatorUserId())
            );
            GetGroupAssetMemberContextResponse.Builder responseBuilder =
                    GetGroupAssetMemberContextResponse.newBuilder().setBr(successBaseResponse());
            if (Objects.isNull(memberContext)) {
                responseBuilder
                        .setActiveMember(Boolean.FALSE)
                        .setRole("")
                        .setUsername("");
            } else {
                responseBuilder
                        .setActiveMember(Boolean.TRUE)
                        .setRole(Objects.nonNull(memberContext.getRole()) ? memberContext.getRole().name() : "")
                        .setUsername(Objects.toString(memberContext.getUsername(), ""));
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(
                    "查询群成员资产上下文系统失败, groupId={}, operatorUserId={}",
                    request.getGroupId(),
                    request.getOperatorUserId(),
                    e
            );
            responseObserver.onNext(GetGroupAssetMemberContextResponse.newBuilder()
                    .setBr(BaseResponse.newBuilder()
                            .setSuccess(Boolean.FALSE)
                            .setCode(ResultEnum.SYSTEM_ERROR.getCode())
                            .setMessage(ResultEnum.SYSTEM_ERROR.getMessage())
                            .build())
                    .setActiveMember(Boolean.FALSE)
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 转换id列表.
     *
     * @param idList id字符串列表
     * @return id列表
     */
    private List<BigInteger> getIdList(final List<String> idList) {
        if (Objects.isNull(idList) || idList.isEmpty()) {
            return List.of();
        }
        return idList.stream()
                .filter(Objects::nonNull)
                .map(BigInteger::new)
                .toList();
    }

    /**
     * 构建会话目标信息成功响应.
     *
     * @param targetInfo 会话目标信息
     * @return 会话目标信息响应
     */
    private ConversationTargetInfoResponse successConversationTargetInfoResponse(
            final ConversationTargetInfo targetInfo
    ) {
        return ConversationTargetInfoResponse.newBuilder()
                .setBr(successBaseResponse())
                .addAllUserInfo(targetInfo.getUserInfoList()
                        .stream()
                        .map(this::toConversationUserInfo)
                        .toList())
                .addAllGroupInfo(targetInfo.getGroupInfoList()
                        .stream()
                        .map(this::toConversationGroupInfo)
                        .toList())
                .build();
    }

    /**
     * 构建会话目标信息失败响应.
     *
     * @param code    状态码
     * @param message 状态信息
     * @return 会话目标信息响应
     */
    private ConversationTargetInfoResponse conversationTargetInfoFailureResponse(
            final Integer code,
            final String message
    ) {
        return ConversationTargetInfoResponse.newBuilder()
                .setBr(BaseResponse.newBuilder()
                        .setSuccess(Boolean.FALSE)
                        .setCode(code)
                        .setMessage(Objects.toString(message, ""))
                        .build())
                .build();
    }

    /**
     * 转换会话用户信息.
     *
     * @param userInfo 用户信息
     * @return 会话用户信息
     */
    private ConversationUserInfo toConversationUserInfo(final ConversationTargetInfo.UserInfo userInfo) {
        return ConversationUserInfo.newBuilder()
                .setUserId(toString(userInfo.getUserId()))
                .setCode(Objects.toString(userInfo.getCode(), ""))
                .setUsername(Objects.toString(userInfo.getUsername(), ""))
                .setAvatarUrl(Objects.toString(userInfo.getAvatarUrl(), ""))
                .setStatus(Objects.nonNull(userInfo.getStatus()) ? userInfo.getStatus().name() : "")
                .build();
    }

    /**
     * 转换会话群聊信息.
     *
     * @param groupInfo 群聊信息
     * @return 会话群聊信息
     */
    private ConversationGroupInfo toConversationGroupInfo(final ConversationTargetInfo.GroupInfo groupInfo) {
        return ConversationGroupInfo.newBuilder()
                .setGroupId(toString(groupInfo.getGroupId()))
                .setName(Objects.toString(groupInfo.getName(), ""))
                .setAvatarUrl(Objects.toString(groupInfo.getAvatarUrl(), ""))
                .setDescription(Objects.toString(groupInfo.getDescription(), ""))
                .setOwnerId(toString(groupInfo.getOwnerId()))
                .setMemberCount(Objects.nonNull(groupInfo.getMemberCount()) ? groupInfo.getMemberCount() : 0L)
                .setStatus(Objects.nonNull(groupInfo.getStatus()) ? groupInfo.getStatus().name() : "")
                .build();
    }

    /**
     * 转换id为字符串.
     *
     * @param id id
     * @return id字符串
     */
    private String toString(final BigInteger id) {
        return Objects.nonNull(id) ? id.toString() : "";
    }
}
