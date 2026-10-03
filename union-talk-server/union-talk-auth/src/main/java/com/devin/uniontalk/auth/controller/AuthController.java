package com.devin.uniontalk.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.auth.vo.req.LoginReqVO;
import com.devin.uniontalk.auth.vo.req.RegisterReqVO;
import com.devin.uniontalk.auth.vo.req.SendCodeReqVO;
import com.devin.uniontalk.auth.vo.resp.LoginRespVO;
import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.cache.constant.CacheConstant;
import com.devin.uniontalk.cache.utils.RedisUtils;
import com.devin.uniontalk.email.service.EmailService;
import com.devin.uniontalk.grpc.user.domain.model.UserInfoProto;
import com.devin.uniontalk.grpc.user.domain.request.CheckUserExistRequest;
import com.devin.uniontalk.grpc.user.domain.request.LoginRequest;
import com.devin.uniontalk.grpc.user.domain.request.RegisterRequest;
import com.devin.uniontalk.grpc.user.domain.response.CheckUserExistResponse;
import com.devin.uniontalk.grpc.user.domain.response.LoginResponse;
import com.devin.uniontalk.grpc.user.domain.response.RegisterResponse;
import com.devin.uniontalk.grpc.user.service.UserGrpcServiceGrpc;
import com.devin.uniontalk.infrastructure.user.enums.DeviceTypeEnum;
import com.devin.uniontalk.web.response.ApiResult;
import com.devin.uniontalk.web.response.ResultEnum;
import com.devin.uniontalk.web.utils.IpUtils;
import com.devin.uniontalk.web.utils.UserAgentUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2026/5/12 22:54.
 *
 * <p>
 * 认证接口
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "auth")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmailService emailService;

    @GrpcClient("union-talk-user")
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userGrpcServiceBlockingStub;

    /**
     * 用户注册.
     *
     * @param registerReqVO 注册参数
     * @return 注册结果
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ApiResult<Boolean> register(
          @Valid @RequestBody final RegisterReqVO registerReqVO
    ) {
        String emailCode = RedisUtils.get(CacheConstant.generateKey(CacheConstant.EMAIL_CODE, registerReqVO.getEmail()));
        if (!StringUtils.hasLength(emailCode) || !emailCode.equals(registerReqVO.getCode())) {
            return ApiResult.fail(ResultEnum.SYSTEM_ERROR.getCode(), "验证码已经过期");
        }

        // 检查用户名或邮箱是否已经被注册
        CheckUserExistResponse checkUserExistResponse = userGrpcServiceBlockingStub.checkUserExist(
                CheckUserExistRequest.newBuilder()
                        .setUsername(registerReqVO.getUsername())
                        .setEmail(registerReqVO.getEmail())
                        .build()
        );
        if (!checkUserExistResponse.getBr().getSuccess()) {
            return ApiResult.fail(checkUserExistResponse.getBr().getCode(), checkUserExistResponse.getBr().getMessage());
        }

        if (checkUserExistResponse.getExist()) {
            return ApiResult.fail(BizErrorEnum.DUPLICATE_ENTITY, "用户名或邮箱已经被注册");
        }

        // 注册
        RegisterResponse registerResponse = userGrpcServiceBlockingStub.register(
                RegisterRequest.newBuilder()
                        .setUsername(registerReqVO.getUsername())
                        .setEmail(registerReqVO.getEmail())
                        .setPassword(registerReqVO.getPassword())
                        .build()
        );

        if (registerResponse.getBr().getSuccess()) {
            // 用户注册成功
            return ApiResult.success(Boolean.TRUE);
        }

        return ApiResult.fail(registerResponse.getBr().getCode(), registerResponse.getBr().getMessage());
    }

    /**
     * 发送邮箱验证码.
     *
     * @param sendCodeReqVO 发送验证码请求参数
     * @return 发送结果
     */
    @PostMapping("/sendCode")
    @Operation(summary = "发送邮箱验证码")
    public ApiResult<Boolean> sendCode(
            @Valid @RequestBody final SendCodeReqVO sendCodeReqVO
    ) {
        try {
            emailService.sendVerificationCode(sendCodeReqVO.getEmail());
            return ApiResult.success(Boolean.TRUE);
        } catch (Exception e) {
            log.error("验证码发送失败: {}", sendCodeReqVO.getEmail(), e);
            return ApiResult.fail(BizErrorEnum.DUPLICATE_REQUEST);
        }
    }

    /**
     * 用户登录.
     *
     * @param loginReqVO 登录请求参数
     * @param request    HTTP请求（用于获取IP和User-Agent）
     * @return 登录结果，包含token和用户基本信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ApiResult<LoginRespVO> login(
            @Valid @RequestBody final LoginReqVO loginReqVO,
            final HttpServletRequest request
    ) {
        // 提取客户端IP和User-Agent
        String ipAddress = IpUtils.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // 从User-Agent解析设备类型和设备名称
        DeviceTypeEnum deviceType = UserAgentUtils.parseDeviceType(userAgent);
        String deviceName = UserAgentUtils.parseDeviceName(userAgent);

        // 通过gRPC调用用户服务校验凭证并记录设备
        LoginResponse loginResponse = userGrpcServiceBlockingStub.login(
                LoginRequest.newBuilder() 
                        .setAccount(loginReqVO.getAccount())
                        .setPassword(loginReqVO.getPassword())
                        .setIpAddress(ipAddress != null ? ipAddress : "")
                        .setUserAgent(userAgent != null ? userAgent : "")
                        .setDeviceType(deviceType.name())
                        .setDeviceName(deviceName)
                        .build()
        );

        if (!loginResponse.getBr().getSuccess()) {
            return ApiResult.fail(loginResponse.getBr().getCode(), loginResponse.getBr().getMessage());
        }

        // 凭证校验通过，使用Sa-Token执行登录
        UserInfoProto userInfo = loginResponse.getUserInfo();
        StpUtil.login(userInfo.getId());

        // 构建登录响应
        LoginRespVO respVO = LoginRespVO.builder()
                .token(StpUtil.getTokenValue())
                .userId(userInfo.getId())
                .username(userInfo.getUsername())
                .email(userInfo.getEmail())
                .avatarUrl(userInfo.getAvatarUrl())
                .build();
        return ApiResult.success(respVO);
    }
}
