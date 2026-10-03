package com.devin.uniontalk.user.domain.entity;

import java.math.BigInteger;
import java.util.Date;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 *  好友申请(FriendRequest)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_friend_request")
public class FriendRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 289490619312569720L;
    
    /**
     * 主键id.
     */
    @TableId
    private BigInteger id; 

    /**
     * 申请用户.
     */
    @TableField("from_user_id")
    private BigInteger fromUserId;      

    /**
     * 目标用户.
     */
    @TableField("to_user_id")
    private BigInteger toUserId;      

    /**
     * 申请信息.
     */
    @TableField("apply_msg")
    private String applyMsg;      

    /**
     * 申请状态.
     */
    @TableField("status")
    private String status;      

    /**
     * 执行时间.
     */
    @TableField("handled_at")
    private Date handledAt;      

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
