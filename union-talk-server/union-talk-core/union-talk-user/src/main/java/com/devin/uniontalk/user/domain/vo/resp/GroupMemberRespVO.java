package com.devin.uniontalk.user.domain.vo.resp;

import com.devin.uniontalk.infrastructure.user.enums.GroupMemberRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigInteger;
import java.util.Date;

/**
 * 2026/05/17 15:10.
 *
 * <p>
 * 群成员信息响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "群成员信息响应参数")
public class GroupMemberRespVO {

    @Schema(description = "用户id")
    private BigInteger userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像地址")
    private String avatarUrl;

    @Schema(description = "群昵称")
    private String nickname;

    @Schema(description = "角色")
    private GroupMemberRoleEnum role;

    @Schema(description = "加入时间")
    private Date joinedAt;
}
