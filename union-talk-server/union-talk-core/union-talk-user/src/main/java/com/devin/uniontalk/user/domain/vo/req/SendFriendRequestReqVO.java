package com.devin.uniontalk.user.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 2026/05/17 15:10.
 *
 * <p>
 * 发送好友申请请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "发送好友申请请求参数")
public class SendFriendRequestReqVO {

    @NotBlank(message = "目标用户code不能为空")
    @Schema(description = "目标用户code")
    private String toUserCode;

    @Size(max = 255, message = "申请信息长度超限")
    @Schema(description = "申请信息")
    private String applyMsg;
}
