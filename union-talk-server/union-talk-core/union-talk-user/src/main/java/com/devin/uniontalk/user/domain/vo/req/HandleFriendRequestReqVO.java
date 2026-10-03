package com.devin.uniontalk.user.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigInteger;

/**
 * 2026/05/17 15:10.
 *
 * <p>
 * 处理好友申请请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "处理好友申请请求参数")
public class HandleFriendRequestReqVO {

    @NotNull(message = "申请id不能为空")
    @Schema(description = "好友申请id")
    private BigInteger requestId;

    @NotNull(message = "处理结果不能为空")
    @Schema(description = "是否接受")
    private Boolean accept;

    @Schema(description = "好友分组id（接受时使用）")
    private BigInteger friendGroupId;

    @Size(max = 64, message = "备注长度超限")
    @Schema(description = "好友备注（接受时使用）")
    private String remark;
}
