package com.devin.uniontalk.message.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import lombok.Data;

/**
 * 2026/05/20 15:20:24.
 *
 * <p>
 *  用户会话(UserConversation)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_user_conversation")
public class UserConversation implements Serializable {

    /**
     * 序列化版本号.
     */
    @Serial
    private static final long serialVersionUID = 767271016914144722L;

    /**
     * 主键id.
     */
    @TableId
    private BigInteger id;

    /**
     * 用户id.
     */
    @TableField("user_id")
    private BigInteger userId;

    /**
     * 会话id.
     */
    @TableField("conversation_id")
    private BigInteger conversationId;

    /**
     * 未读统计.
     */
    @TableField("unread_count")
    private Integer unreadCount;

    /**
     * 已读消息id.
     */
    @TableField("last_read_msg_id")
    private BigInteger lastReadMsgId;

    /**
     * 提及未读数量.
     */
    @TableField("mention_unread_count")
    private Integer mentionUnreadCount;

    /**
     * 最后提及消息id.
     */
    @TableField("last_mention_msg_id")
    private BigInteger lastMentionMsgId;

    /**
     * 是否置顶.
     */
    @TableField("is_pinned")
    private Boolean isPinned;

    /**
     * 是否免打扰.
     */
    @TableField("is_muted")
    private Boolean isMuted;

    /**
     * 会话状态.
     */
    @TableField("status")
    private String status;

    /**
     * 会话删除时间.
     */
    @TableField("hidden_at")
    private Date hiddenAt;

    /**
     * 创建时间.
     */
    @TableField("created_at")
    private Date createdAt;

    /**
     * 更新时间.
     */
    @TableField("updated_at")
    private Date updatedAt;

    /**
     * 初始化.
     */
    public void init() {
        this.mentionUnreadCount = 0;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
}
