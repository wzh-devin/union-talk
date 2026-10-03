package com.devin.uniontalk.user.domain.entity;

import java.math.BigInteger;
import java.util.Date;
import java.util.Optional;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 2026/05/12 22:01:35.
 *
 * <p>
 *  用户表(User)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_user")
public class User implements Serializable {

    /**
     * 序列化版本号.
     */
    @Serial
    private static final long serialVersionUID = 401530141018008832L;
    
    /**
     * 主键id.
     */
    @TableId
    private BigInteger id; 

    /**
     * 用户唯一CODE.
     */
    @TableField("code")
    private String code;      

    /**
     * 获取不含数据库固定字段填充空格的用户唯一CODE.
     *
     * @return 用户唯一CODE
     */
    public String getCode() {
        return Optional.ofNullable(code).map(String::stripTrailing).orElse(null);
    }

    /**
     * 注册方式.
     */
    @TableField("register_way")
    private String registerWay;      

    /**
     * 用户名.
     */
    @TableField("username")
    private String username;      

    /**
     * 密码.
     */
    @TableField("password")
    private String password;      

    /**
     * 邮箱.
     */
    @TableField("email")
    private String email;      

    /**
     * 头像地址.
     */
    @TableField("avatar_url")
    private String avatarUrl;      

    /**
     * 简介.
     */
    @TableField("bio")
    private String bio;      

    /**
     * 添加好友是否需要验证.
     */
    @TableField("need_friend_verify")
    private Boolean needFriendVerify;      

    /**
     * 用户账号状态.
     */
    @TableField("status")
    private String status;      

    /**
     * 最近登录时间.
     */
    @TableField("last_login_at")
    private Date lastLoginAt;      

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

    /**
     * 登录，更新最近登录时间.
     */
    public void login() {
        this.lastLoginAt = new Date();
        this.updatedAt = new Date();
    }

    /**
     * 修改加密后的用户密码.
     *
     * @param encryptedPassword 加密后的新密码
     */
    public void changePassword(final String encryptedPassword) {
        this.password = encryptedPassword;
        this.updatedAt = new Date();
    }
}
