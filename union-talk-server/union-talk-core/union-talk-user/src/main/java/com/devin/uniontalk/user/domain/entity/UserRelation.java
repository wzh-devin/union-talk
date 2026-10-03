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
 *  用户关系(UserRelation)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_user_relation")
public class UserRelation implements Serializable {
    @Serial
    private static final long serialVersionUID = -94168281400488020L;
    
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
     * 对方id.
     */
    @TableField("target_id")
    private BigInteger targetId;      

    /**
     * 关系状态.
     */
    @TableField("status")
    private String status;      

    /**
     * 备注.
     */
    @TableField("remark")
    private String remark;      

    /**
     * 对方所在分组.
     */
    @TableField("friend_group_id")
    private BigInteger friendGroupId;      

    /**
     * 删除时间.
     */
    @TableField("deleted_at")
    private Date deletedAt;      

    /**
     * 拉黑时间.
     */
    @TableField("blocked_at")
    private Date blockedAt;      

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
