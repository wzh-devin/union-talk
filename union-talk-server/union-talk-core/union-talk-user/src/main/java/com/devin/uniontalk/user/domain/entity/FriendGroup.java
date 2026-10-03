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
 *  好友分组(FriendGroup)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_friend_group")
public class FriendGroup implements Serializable {
    @Serial
    private static final long serialVersionUID = -23052785740245354L;
    
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
     * 分组名称.
     */
    @TableField("name")
    private String name;      

    /**
     * 分组排序.
     */
    @TableField("sort_order")
    private Integer sortOrder;      

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
