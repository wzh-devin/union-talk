package com.devin.uniontalk.auth.vo.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/5/16 22:00.
 *
 * <p>
 * 登录响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "登录响应参数")
public class LoginRespVO {

    @Schema(description = "登录令牌")
    private String token;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像地址")
    private String avatarUrl;
}
