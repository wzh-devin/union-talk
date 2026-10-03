package com.devin.uniontalk.email.service;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.exception.BizException;
import com.devin.uniontalk.cache.constant.CacheConstant;
import com.devin.uniontalk.cache.utils.RedisUtils;
import com.devin.uniontalk.email.properties.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 2026/05/16.
 *
 * <p>
 * 邮箱服务，提供验证码发送能力
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public record EmailService(JavaMailSender mailSender, EmailProperties emailProperties) {

    /**
     * 发送验证码到指定邮箱.
     *
     * @param email 目标邮箱地址
     */
    public void sendVerificationCode(final String email) throws MessagingException {
        // 判断验证码是否被重复发送，防止盗刷
        String emailCode = RedisUtils.get(CacheConstant.generateKey(CacheConstant.EMAIL_CODE, email));
        if (StringUtils.hasLength(emailCode)) {
            throw new BizException(BizErrorEnum.DUPLICATE_REQUEST);
        }

        // 生成6位随机验证码
        String code = generateCode();

        // 构建并发送 HTML 邮件
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(emailProperties.getFrom());
        helper.setTo(email);
        helper.setSubject("Union Talk 验证码");
        helper.setText(buildHtmlContent(code), true);
        mailSender.send(message);

        // 存入 Redis，设置过期时间
        String key = CacheConstant.generateKey(CacheConstant.EMAIL_CODE, email);
        RedisUtils.setEx(key, code, emailProperties.getCodeExpireSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 生成6位随机数字验证码.
     *
     * @return 验证码字符串
     */
    private String generateCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

    /**
     * 构建验证码邮件 HTML 内容.
     *
     * @param code 验证码
     * @return HTML 字符串
     */
    private String buildHtmlContent(final String code) {
        return """
                <div style="max-width:400px;margin:0 auto;padding:24px;font-family:sans-serif;">
                    <h2 style="color:#333;">Union Talk 验证码</h2>
                    <p style="color:#666;font-size:14px;">您的验证码为：</p>
                    <p style="font-size:32px;font-weight:bold;letter-spacing:8px;color:#1a73e8;">%s</p>
                    <p style="color:#999;font-size:12px;">验证码 %d 秒内有效，请勿泄露给他人。</p>
                </div>
                """.formatted(code, emailProperties.getCodeExpireSeconds());
    }
}
