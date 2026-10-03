package com.devin.uniontalk.user.domain.vo.resp;

import com.devin.uniontalk.infrastructure.user.enums.FriendRequestStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigInteger;
import java.util.Date;

/**
 * 2026/05/17 15:10.
 *
 * <p>
 * 好友申请响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "好友申请响应参数")
public class FriendRequestRespVO {

    @Schema(description = "申请id")
    private BigInteger id;

    @Schema(description = "申请用户id")
    private BigInteger fromUserId;

    @Schema(description = "申请用户名")
    private String fromUsername;

    @Schema(description = "申请用户头像")
    private String fromAvatarUrl;

    @Schema(description = "申请信息")
    private String applyMsg;

    @Schema(description = "申请状态")
    private FriendRequestStatusEnum status;

    @Schema(description = "创建时间")
    private Date createdAt;
}
