package com.devin.uniontalk.email.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 2026/05/16.
 *
 * <p>
 * 邮箱服务配置属性
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "union.email")
public class EmailProperties {

    /**
     * SMTP 服务器地址.
     */
    private String host = "smtp.qq.com";

    /**
     * SMTP 服务器端口.
     */
    private Integer port = 465;

    /**
     * 发件人账号.
     */
    private String username;

    /**
     * 发件人授权码/密码.
     */
    private String password;

    /**
     * 发件人邮箱地址.
     */
    private String from;

    /**
     * 邮件传输协议.
     */
    private String protocol = "smtps";

    /**
     * 验证码有效期（秒）.
     */
    private Integer codeExpireSeconds = 60;
}
