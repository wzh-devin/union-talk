package com.devin.uniontalk.auth.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 2026/5/12 23:00.
 *
 * <p>
 * 注册请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "注册请求参数")
public class RegisterReqVO {

    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(max = 48, message = "用户名长度超限")
    private String username;

    @Schema(description = "邮箱")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不符合要求")
    private String email;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    @Size(min = 10, max = 16, message = "密码长度需要在10～16位")
    private String password;

    @Schema(description = "验证码")
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码需要为6位数字")
    private String code;
}
