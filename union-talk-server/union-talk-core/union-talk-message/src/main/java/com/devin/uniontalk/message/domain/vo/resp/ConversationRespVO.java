package com.devin.uniontalk.message.domain.vo.resp;

import com.devin.uniontalk.infrastructure.message.enums.ConversationTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.UserConversationStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.util.Date;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/05/20 16:00.
 *
 * <p>
 * 会话响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "会话响应参数")
public class ConversationRespVO {

    /**
     * 用户会话id.
     */
    @Schema(description = "用户会话id")
    private BigInteger userConversationId;

    /**
     * 会话id.
     */
    @Schema(description = "会话id")
    private BigInteger conversationId;

    /**
     * 会话类型.
     */
    @Schema(description = "会话类型")
    private ConversationTypeEnum type;

    /**
     * 群聊id.
     */
    @Schema(description = "群聊id")
    private BigInteger groupId;

    /**
     * 最后一条消息id.
     */
    @Schema(description = "最后一条消息id")
    private BigInteger lastMsgId;

    /**
     * 最后一条消息时间.
     */
    @Schema(description = "最后一条消息时间")
    private Date lastMsgAt;

    /**
     * 未读数量.
     */
    @Schema(description = "未读数量")
    private Integer unreadCount;

    /**
     * 最后已读消息id.
     */
    @Schema(description = "最后已读消息id")
    private BigInteger lastReadMsgId;

    /**
     * 提及未读数量.
     */
    @Schema(description = "提及未读数量")
    private Integer mentionUnreadCount;

    /**
     * 最后提及消息id.
     */
    @Schema(description = "最后提及消息id")
    private BigInteger lastMentionMsgId;

    /**
     * 是否置顶.
     */
    @Schema(description = "是否置顶")
    private Boolean isPinned;

    /**
     * 是否免打扰.
     */
    @Schema(description = "是否免打扰")
    private Boolean isMuted;

    /**
     * 用户会话状态.
     */
    @Schema(description = "用户会话状态")
    private UserConversationStatusEnum status;

    /**
     * 用户会话更新时间.
     */
    @Schema(description = "用户会话更新时间")
    private Date updatedAt;

    /**
     * 最后一条消息.
     */
    @Schema(description = "最后一条消息")
    private MessageRespVO lastMessage;

    /**
     * 私聊目标用户信息.
     */
    @Schema(description = "私聊目标用户信息")
    private ConversationUserInfoRespVO targetUser;

    /**
     * 群聊信息.
     */
    @Schema(description = "群聊信息")
    private ConversationGroupInfoRespVO groupInfo;
}
