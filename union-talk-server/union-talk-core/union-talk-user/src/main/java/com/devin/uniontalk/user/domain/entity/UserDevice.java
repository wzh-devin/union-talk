package com.devin.uniontalk.user.domain.entity;

import java.math.BigInteger;
import java.util.Date;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devin.uniontalk.datasource.handler.InetTypeHandler;

/**
 * 2026/05/12 22:01:39.
 *
 * <p>
 *  用户登录设备(UserDevice)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_user_device", autoResultMap = true)
public class UserDevice implements Serializable {
    @Serial
    private static final long serialVersionUID = 814042162461457920L;

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
     * 设备名称.
     */
    @TableField("device_name")
    private String deviceName;

    /**
     * 设备类型.
     */
    @TableField("device_type")
    private String deviceType;

    /**
     * IP地址.
     */
    @TableField(value = "ip_address", typeHandler = InetTypeHandler.class)
    private String ipAddress;

    /**
     * AGENT.
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 最后的激活时间.
     */
    @TableField("last_active_at")
    private Date lastActiveAt;

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
        this.lastActiveAt = new Date();
    }

    /**
     * 记录登录活跃，更新最后活跃时间.
     */
    public void recordActive() {
        this.lastActiveAt = new Date();
        this.updatedAt = new Date();
    }
}
