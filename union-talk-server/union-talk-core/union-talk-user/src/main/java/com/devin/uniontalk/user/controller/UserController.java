package com.devin.uniontalk.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.user.domain.entity.User;
import com.devin.uniontalk.user.domain.entity.convertor.UserConvertor;
import com.devin.uniontalk.user.domain.vo.req.ResetPasswordReqVO;
import com.devin.uniontalk.user.domain.vo.req.UpdateUserReqVO;
import com.devin.uniontalk.user.domain.vo.resp.UserInfoRespVO;
import com.devin.uniontalk.user.service.UserService;
import com.devin.uniontalk.web.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigInteger;

/**
 * 2026/5/12 22:10.
 *
 * <p>
 * 用户相关接口
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "User")
@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class UserController {

    /**
     * 用户服务.
     */
    private final UserService userService;

    /**
     * 更新用户信息.
     *
     * @param reqVO 更新用户信息请求参数
     * @return 更新结果
     */
    @PutMapping("/updateUser")
    @Operation(summary = "更新用户信息")
    public ApiResult<Boolean> updateUser(
            @Valid @RequestBody final UpdateUserReqVO reqVO
    ) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        userService.updateUser(
                userId,
                reqVO.getUsername(),
                reqVO.getAvatarUrl(),
                reqVO.getBio(),
                reqVO.getNeedFriendVerify()
        );
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 修改当前用户密码.
     *
     * @param reqVO 修改密码请求参数
     * @return 修改结果
     */
    @PostMapping("/resetPassword")
    @Operation(summary = "修改密码")
    public ApiResult<Void> resetPassword(
            @Valid @RequestBody final ResetPasswordReqVO reqVO
    ) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        userService.resetPassword(userId, reqVO.getCurrentPassword(), reqVO.getNewPassword());
        return ApiResult.success();
    }

    /**
     * 获取当前用户信息.
     *
     * @return 用户信息
     */
    @GetMapping("/getCurrentUserInfo")
    @Operation(summary = "获取当前用户信息")
    public ApiResult<UserInfoRespVO> getCurrentUserInfo() {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        User user = userService.getUserById(userId);
        return ApiResult.success(UserConvertor.INSTANCE.toUserInfoRespVO(user));
    }

    /**
     * 根据用户唯一code搜索用户.
     *
     * @param code code
     * @return UserInfo
     */
    @GetMapping("/getUserInfo")
    @Operation(summary = "根据用户唯一code搜索用户")
    public ApiResult<UserInfoRespVO> getUserInfo(
            @Parameter(name = "code", description = "用户ID码") @RequestParam("code") final String code
    ) {
        User user = userService.getUserByCode(code);
        return ApiResult.success(UserConvertor.INSTANCE.toUserInfoRespVO(user));
    }

    /**
     * 上传用户头像.
     *
     * @param file 头像文件
     * @return 头像地址
     */
    @PostMapping("/uploadAvatar")
    @Operation(summary = "上传头像")
    public ApiResult<String> uploadAvatar(
            @RequestPart("file") @Parameter(name = "file") final MultipartFile file
    ) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        String avatarUrl = userService.uploadAvatar(userId, file);
        return ApiResult.success(avatarUrl);
    }
}
