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
 *  群成员(GroupMember)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_group_member")
public class GroupMember implements Serializable {
    @Serial
    private static final long serialVersionUID = 489848189935391363L;
    
    /**
     * 主键id.
     */
    @TableId
    private BigInteger id; 

    /**
     * 群组id.
     */
    @TableField("group_id")
    private BigInteger groupId;      

    /**
     * 用户id.
     */
    @TableField("user_id")
    private BigInteger userId;      

    /**
     * 用户角色.
     */
    @TableField("role")
    private String role;      

    /**
     * 群昵称.
     */
    @TableField("nickname")
    private String nickname;      

    /**
     * 加入时间.
     */
    @TableField("joined_at")
    private Date joinedAt;      

    /**
     * 离开时间.
     */
    @TableField("left_at")
    private Date leftAt;      

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
