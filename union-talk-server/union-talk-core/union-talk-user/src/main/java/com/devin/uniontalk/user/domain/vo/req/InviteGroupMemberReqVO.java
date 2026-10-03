package com.devin.uniontalk.user.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigInteger;
import java.util.List;

/**
 * 2026/05/17 15:10.
 *
 * <p>
 * 邀请入群请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "邀请入群请求参数")
public class InviteGroupMemberReqVO {

    @NotNull(message = "群聊id不能为空")
    @Schema(description = "群聊id")
    private BigInteger groupId;

    @NotEmpty(message = "邀请用户列表不能为空")
    @Schema(description = "邀请用户id列表")
    private List<BigInteger> uidList;
}
