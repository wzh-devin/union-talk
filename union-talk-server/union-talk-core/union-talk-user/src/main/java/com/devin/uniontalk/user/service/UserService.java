package com.devin.uniontalk.user.service;

import com.devin.uniontalk.infrastructure.user.enums.DeviceTypeEnum;
import com.devin.uniontalk.user.domain.entity.User;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigInteger;

/**
 * 2026/05/12 22:01:38.
 *
 * <p>
 *  用户表(User)Service层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface UserService {

    /**
     * 检查用户是否存在.
     *
     * @param username 用户名
     * @param email 邮箱
     * @return 用户是否存在
     */
    Boolean checkUserExist(String username, String email);

    /**
     * 注册用户.
     *
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     * @return 用户信息
     */
    User register(String username, String email, String password);

    /**
     * 用户登录，校验账号和密码，并记录登录设备.
     *
     * @param account    登录账号（用户名或邮箱）
     * @param password   密码
     * @param ipAddress  IP地址
     * @param userAgent  User-Agent
     * @param deviceType 设备类型
     * @param deviceName 设备名称
     * @return 用户信息
     */
    User login(
            String account,
            String password,
            String ipAddress,
            String userAgent,
            DeviceTypeEnum deviceType,
            String deviceName
    );

    /**
     * 更新用户信息.
     *
     * @param userId          用户ID
     * @param username        用户名（可选）
     * @param avatarUrl       头像地址（可选）
     * @param bio             简介（可选）
     * @param needFriendVerify 是否需要好友验证（可选）
     * @return 更新后的用户信息
     */
    User updateUser(BigInteger userId, String username, String avatarUrl, String bio, Boolean needFriendVerify);

    /**
     * 修改当前用户密码.
     *
     * @param userId          用户ID
     * @param currentPassword 当前密码
     * @param newPassword     新密码
     */
    void resetPassword(BigInteger userId, String currentPassword, String newPassword);

    /**
     * 根据用户ID获取用户信息.
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(BigInteger userId);

    /**
     * 根据用户Code获取用户信息.
     * @param code 用户Code
     * @return 用户信息
     */
    User getUserByCode(String code);

    /**
     * 上传用户头像.
     *
     * @param userId 用户ID
     * @param file   头像文件
     * @return 头像地址
     */
    String uploadAvatar(BigInteger userId, MultipartFile file);
}
