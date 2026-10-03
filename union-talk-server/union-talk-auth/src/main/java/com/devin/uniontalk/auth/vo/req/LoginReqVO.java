package com.devin.uniontalk.auth.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 2026/5/16 22:00.
 *
 * <p>
 * 登录请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "登录请求参数")
public class LoginReqVO {

    @Schema(description = "登录账号（用户名或邮箱）")
    @NotBlank(message = "登录账号不能为空")
    @Size(max = 48, message = "登录账号长度超限")
    private String account;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    @Size(min = 10, max = 16, message = "密码长度需要在10～16位")
    private String password;
}
