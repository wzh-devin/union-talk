package com.devin.uniontalk.message.domain.vo.resp;

import com.devin.uniontalk.infrastructure.user.enums.UserStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/05/31 21:40.
 *
 * <p>
 * 会话用户信息响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "会话用户信息响应参数")
public class ConversationUserInfoRespVO {

    /**
     * 用户id.
     */
    @Schema(description = "用户id")
    private BigInteger userId;

    /**
     * 用户code.
     */
    @Schema(description = "用户code")
    private String code;

    /**
     * 用户名.
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 头像地址.
     */
    @Schema(description = "头像地址")
    private String avatarUrl;

    /**
     * 用户状态.
     */
    @Schema(description = "用户状态")
    private UserStatusEnum status;
}
