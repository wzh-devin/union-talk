package com.devin.uniontalk.base.utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * 2026/5/13 15:10.
 *
 * <p>
 * 通用加解密工具
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public final class CryptoUtils {

    private static final String AES_ALGORITHM = "AES";

    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final int GCM_IV_LENGTH = 12;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private static final Base64.Decoder TOKEN_DECODER = Base64.getUrlDecoder();

    private static SecretKeySpec secretKey;

    /**
     * 初始化密钥.
     *
     * @param key 密钥
     */
    public void init(final String key) {
        byte[] keyBytes = resolveKeyBytes(key);
        try {
            secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
        } finally {
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    /**
     * 加密文本.
     *
     * @param plainText 明文
     * @return 密文令牌
     */
    public static String encrypt(final String plainText) {
        if (plainText == null) {
            throw new IllegalArgumentException("待加密文本不能为空");
        }
        return encryptBytes(plainText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解密文本.
     *
     * @param encryptedText 密文令牌
     * @return 明文
     */
    public static String decrypt(final String encryptedText) {
        return new String(decryptBytes(encryptedText), StandardCharsets.UTF_8);
    }

    /**
     * 加密字节数组.
     *
     * <p>
     * 令牌格式为Base64Url(iv + ciphertext)，IV固定12字节拼接在密文头部。
     * </p>
     *
     * @param plainBytes 明文字节数组
     * @return 密文令牌
     */
    public static String encryptBytes(final byte[] plainBytes) {
        if (plainBytes == null) {
            throw new IllegalArgumentException("待加密字节数组不能为空");
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);
        try {
            // 每次加密创建独立Cipher实例，避免共享Cipher带来的并发问题。
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plainBytes);
            // IV固定12字节拼接在密文头部，一起Base64Url编码为令牌。
            byte[] token = new byte[GCM_IV_LENGTH + cipherBytes.length];
            System.arraycopy(iv, 0, token, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherBytes, 0, token, GCM_IV_LENGTH, cipherBytes.length);
            return TOKEN_ENCODER.encodeToString(token);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("文本加密失败", ex);
        }
    }

    /**
     * 解密字节数组.
     *
     * <p>
     * 令牌格式为Base64Url(iv + ciphertext)，IV固定12字节拼接在密文头部。
     * </p>
     *
     * @param encryptedText 密文令牌
     * @return 明文字节数组
     */
    public static byte[] decryptBytes(final String encryptedText) {
        if (isBlank(encryptedText)) {
            throw new IllegalArgumentException("待解密密文不能为空");
        }
        try {
            // 解码后按固定偏移量切分IV与密文。
            byte[] token = TOKEN_DECODER.decode(encryptedText);
            if (token.length <= GCM_IV_LENGTH) {
                throw new IllegalArgumentException("密文格式不正确");
            }
            byte[] iv = Arrays.copyOfRange(token, 0, GCM_IV_LENGTH);
            byte[] cipherBytes = Arrays.copyOfRange(token, GCM_IV_LENGTH, token.length);
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return cipher.doFinal(cipherBytes);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("密文Base64编码不正确", ex);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("文本解密失败", ex);
        }
    }

    /**
     * 解析AES密钥字节数组.
     *
     * <p>
     * 长度为16、24、32字节时直接使用，其他长度使用SHA-256规整为32字节。
     * </p>
     *
     * @param keyText 密钥文本
     * @return AES密钥字节数组
     */
    private static byte[] resolveKeyBytes(final String keyText) {
        if (isBlank(keyText)) {
            throw new IllegalArgumentException("加解密密钥不能为空");
        }
        byte[] rawBytes = keyText.trim().getBytes(StandardCharsets.UTF_8);
        if (isAesKeyLength(rawBytes.length)) {
            return rawBytes;
        }
        return sha256(rawBytes);
    }

    /**
     * 判断是否为AES密钥长度.
     *
     * @param length 密钥长度
     * @return true表示是AES密钥长度
     */
    private static boolean isAesKeyLength(final int length) {
        return length == 16 || length == 24 || length == 32;
    }

    /**
     * 计算SHA-256摘要.
     *
     * @param bytes 原始字节数组
     * @return SHA-256摘要字节数组
     */
    private static byte[] sha256(final byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("加解密密钥规整失败", ex);
        } finally {
            // 规整完成后清理原始密钥字节数组，避免临时数组继续留存。
            Arrays.fill(bytes, (byte) 0);
        }
    }

    /**
     * 判断字符串是否为空白.
     *
     * @param value 字符串
     * @return true表示为空白
     */
    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }
}