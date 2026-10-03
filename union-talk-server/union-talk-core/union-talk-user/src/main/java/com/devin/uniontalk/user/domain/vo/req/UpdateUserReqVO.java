package com.devin.uniontalk.user.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 2026/5/16 23:40.
 *
 * <p>
 * 更新用户信息请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "更新用户信息请求参数")
public class UpdateUserReqVO {

    @Schema(description = "用户名")
    @Size(max = 32, message = "用户名长度超限")
    private String username;

    @Schema(description = "头像地址")
    private String avatarUrl;

    @Schema(description = "简介")
    @Size(max = 255, message = "简介长度超限")
    private String bio;

    @Schema(description = "添加好友是否需要验证")
    private Boolean needFriendVerify;
}
