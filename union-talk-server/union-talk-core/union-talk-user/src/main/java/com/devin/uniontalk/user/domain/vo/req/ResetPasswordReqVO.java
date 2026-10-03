package com.devin.uniontalk.user.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 2026/7/11 17:12.
 *
 * <p>
 * 更新密码请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "更新密码请求参数")
public class ResetPasswordReqVO {

    /**
     * 当前密码.
     */
    @Schema(description = "当前密码")
    @NotBlank(message = "当前密码不能为空")
    @Size(min = 10, max = 16, message = "密码长度需要在10～16位")
    private String currentPassword;

    /**
     * 新密码.
     */
    @Schema(description = "新密码")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 10, max = 16, message = "密码长度需要在10～16位")
    private String newPassword;
}
