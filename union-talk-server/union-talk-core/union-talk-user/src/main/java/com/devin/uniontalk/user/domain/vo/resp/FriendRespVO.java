package com.devin.uniontalk.user.domain.vo.resp;

import com.devin.uniontalk.infrastructure.user.enums.UserRelationStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigInteger;

/**
 * 2026/05/17 15:10.
 *
 * <p>
 * 好友信息响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "好友信息响应参数")
public class FriendRespVO {

    @Schema(description = "用户id")
    private BigInteger userId;

    @Schema(description = "用户code")
    private String code;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像地址")
    private String avatarUrl;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "好友分组id")
    private BigInteger friendGroupId;

    @Schema(description = "关系状态")
    private UserRelationStatusEnum status;
}
