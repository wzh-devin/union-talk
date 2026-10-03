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
 * 2026/05/12 22:01:41.
 *
 * <p>
 *  用户设置(UserSetting)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_user_setting")
public class UserSetting implements Serializable {
    @Serial
    private static final long serialVersionUID = -82607965797037947L;
    
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
     * 配置Key.
     */
    @TableField("key")
    private String key;      

    /**
     * 配置Value.
     */
    @TableField("value")
    private String value;      

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
