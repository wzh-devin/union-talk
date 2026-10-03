package com.devin.uniontalk.email.config;

import com.devin.uniontalk.email.properties.EmailProperties;
import com.devin.uniontalk.email.service.EmailService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.util.Properties;

/**
 * 2026/05/16.
 *
 * <p>
 * 邮箱服务自动配置
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfiguration {

    /**
     * 配置 JavaMailSender.
     *
     * @param emailProperties 邮箱配置属性
     * @return JavaMailSender
     */
    @Bean
    public JavaMailSender javaMailSender(final EmailProperties emailProperties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(emailProperties.getHost());
        mailSender.setPort(emailProperties.getPort());
        mailSender.setUsername(emailProperties.getUsername());
        mailSender.setPassword(emailProperties.getPassword());
        mailSender.setProtocol(emailProperties.getProtocol());

        // SMTP 连接属性
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.connectiontimeout", "5000");
        return mailSender;
    }

    /**
     * 注册邮箱服务.
     * @param javaMailSender JavaMailSender
     * @param emailProperties 邮箱配置属性
     * @return 邮箱服务
     */
    @Bean
    public EmailService emailService(
            final JavaMailSender javaMailSender,
            final EmailProperties emailProperties
    ) {
        return new EmailService(javaMailSender, emailProperties);
    }
}
