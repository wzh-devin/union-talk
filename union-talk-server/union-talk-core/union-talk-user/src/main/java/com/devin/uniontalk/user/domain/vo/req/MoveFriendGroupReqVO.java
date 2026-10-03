package com.devin.uniontalk.user.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigInteger;

/**
 * 2026/05/17 15:10.
 *
 * <p>
 * 移动好友到分组请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "移动好友到分组请求参数")
public class MoveFriendGroupReqVO {

    @NotNull(message = "目标用户id不能为空")
    @Schema(description = "目标用户id")
    private BigInteger targetId;

    @NotNull(message = "分组id不能为空")
    @Schema(description = "好友分组id")
    private BigInteger friendGroupId;
}
