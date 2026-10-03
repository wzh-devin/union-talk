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
 *  群聊(Group)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_group")
public class Group implements Serializable {
    @Serial
    private static final long serialVersionUID = -92985939129586384L;
    
    /**
     * 主键id.
     */
    @TableId
    private BigInteger id; 

    /**
     * 群主id.
     */
    @TableField("owner_id")
    private BigInteger ownerId;      

    /**
     * 群聊名称.
     */
    @TableField("name")
    private String name;      

    /**
     * 群聊头像.
     */
    @TableField("avatar_url")
    private String avatarUrl;      

    /**
     * 群聊描述.
     */
    @TableField("description")
    private String description;      

    /**
     * 人员限制.
     */
    @TableField("member_limit")
    private Integer memberLimit;      

    /**
     * 群聊状态.
     */
    @TableField("status")
    private String status;      

    /**
     * 解散时间.
     */
    @TableField("dissolved_at")
    private String dissolvedAt;      

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
