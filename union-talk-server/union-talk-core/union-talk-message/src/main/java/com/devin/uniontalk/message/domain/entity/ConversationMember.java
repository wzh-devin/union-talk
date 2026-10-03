package com.devin.uniontalk.message.domain.entity;

import java.math.BigInteger;
import java.util.Date;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 2026/05/20 15:20:24.
 *
 * <p>
 *  私聊会话成员(ConversationMember)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_conversation_member")
public class ConversationMember implements Serializable {
    @Serial
    private static final long serialVersionUID = -84454655242541721L;
    
    /**
     * 主键id.
     */
    @TableId
    private BigInteger id; 

    /**
     * 会话id.
     */
    @TableField("conversation_id")
    private BigInteger conversationId;      

    /**
     * 用户id.
     */
    @TableField("user_id")
    private BigInteger userId;      

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
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
}
