package com.devin.uniontalk.user.domain.vo.resp;

import com.devin.uniontalk.infrastructure.user.enums.UserStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.Date;

/**
 * 2026/5/17 00:00.
 *
 * <p>
 * 用户信息响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "用户信息响应参数")
public class UserInfoRespVO {

    @Schema(description = "用户id")
    private String uid;

    @Schema(description = "用户唯一CODE")
    private String code;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像地址")
    private String avatarUrl;

    @Schema(description = "简介")
    private String bio;

    @Schema(description = "添加好友是否需要验证")
    private Boolean needFriendVerify;

    @Schema(description = "用户状态")
    private UserStatusEnum status;

    @Schema(description = "最近登录时间")
    private Date lastLoginAt;
}
