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
 * 2026/05/20 15:20:19.
 *
 * <p>
 *  会话表(Conversation)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_conversation")
public class Conversation implements Serializable {
    @Serial
    private static final long serialVersionUID = -91178482762142589L;
    
    /**
     * 主键id.
     */
    @TableId
    private BigInteger id; 

    /**
     * 会话类型.
     */
    @TableField("type")
    private String type;      

    /**
     * 群聊id.
     */
    @TableField("group_id")
    private BigInteger groupId;      

    /**
     * 最后一条消息id.
     */
    @TableField("last_msg_id")
    private BigInteger lastMsgId;      

    /**
     * 最后一条消息时间.
     */
    @TableField("last_msg_at")
    private Date lastMsgAt;      

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
