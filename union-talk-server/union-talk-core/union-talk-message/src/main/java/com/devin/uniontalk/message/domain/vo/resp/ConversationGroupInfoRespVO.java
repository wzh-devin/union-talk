package com.devin.uniontalk.message.domain.vo.resp;

import com.devin.uniontalk.infrastructure.user.enums.GroupStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/05/31 21:40.
 *
 * <p>
 * 会话群聊信息响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "会话群聊信息响应参数")
public class ConversationGroupInfoRespVO {

    /**
     * 群聊id.
     */
    @Schema(description = "群聊id")
    private BigInteger groupId;

    /**
     * 群聊名称.
     */
    @Schema(description = "群聊名称")
    private String name;

    /**
     * 群聊头像.
     */
    @Schema(description = "群聊头像")
    private String avatarUrl;

    /**
     * 群聊描述.
     */
    @Schema(description = "群聊描述")
    private String description;

    /**
     * 群主id.
     */
    @Schema(description = "群主id")
    private BigInteger ownerId;

    /**
     * 当前成员数.
     */
    @Schema(description = "当前成员数")
    private Long memberCount;

    /**
     * 群聊状态.
     */
    @Schema(description = "群聊状态")
    private GroupStatusEnum status;
}
