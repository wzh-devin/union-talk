package com.devin.uniontalk.user.service.impl;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.base.utils.CryptoUtils;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.infrastructure.user.enums.DeviceTypeEnum;
import com.devin.uniontalk.infrastructure.user.enums.RegisterWayEnum;
import com.devin.uniontalk.infrastructure.user.enums.UserStatusEnum;
import com.devin.uniontalk.user.dao.FriendGroupDao;
import com.devin.uniontalk.user.dao.UserDao;
import com.devin.uniontalk.user.dao.UserDeviceDao;
import com.devin.uniontalk.user.domain.entity.User;
import com.devin.uniontalk.user.grpc.client.FileGrpcClient;
import com.devin.uniontalk.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigInteger;

/**
 * 2026/05/12 22:01:38.
 *
 * <p>
 * 用户表(User)ServiceImpl层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /**
     * 用户数据访问对象.
     */
    private final UserDao userDao;

    /**
     * 用户设备数据访问对象.
     */
    private final UserDeviceDao userDeviceDao;

    /**
     * 好友分组数据访问对象.
     */
    private final FriendGroupDao friendGroupDao;

    /**
     * 文件 gRPC 客户端.
     */
    private final FileGrpcClient fileGrpcClient;

    @Override
    public Boolean checkUserExist(final String username, final String email) {
        return userDao.checkUserExist(username, email);
    }

    @Override
    public User register(
            final String username,
            final String email,
            final String password
    ) {
        User user = new User();
        user.setId(IdGenerator.nextIdBigInteger());
        user.setCode(IdGenerator.nextKey(16));
        user.setPassword(CryptoUtils.encrypt(password));
        user.setEmail(email);
        user.setUsername(username);
        user.setAvatarUrl("");
        user.setNeedFriendVerify(Boolean.FALSE);
        user.setStatus(UserStatusEnum.NORMAL.name());
        user.setBio("");
        user.setRegisterWay(RegisterWayEnum.EMAIL.name());
        user.init();
        userDao.save(user);

        // 初始化用户默认好友分组
        friendGroupDao.init(user.getId());

        return user;
    }

    @Override
    public User login(
            final String account,
            final String password,
            final String ipAddress,
            final String userAgent,
            final DeviceTypeEnum deviceType,
            final String deviceName
    ) {
        // 根据账号（用户名或邮箱）查询用户
        User user = userDao.getByAccount(account);
        AssertUtils.nonNull(user, BizErrorEnum.USER_NOT_FOUND);

        // 校验用户状态
        AssertUtils.notEqual(UserStatusEnum.BLOCKED.name(), user.getStatus(), BizErrorEnum.USER_BLOCKED);

        // 解密存储的密码并与输入密码比对
        String decryptedPassword = CryptoUtils.decrypt(user.getPassword());
        AssertUtils.equal(password, decryptedPassword, BizErrorEnum.PASSWORD_ERROR);

        // 更新登录时间
        user.login();
        userDao.updateById(user);

        // 记录登录设备信息
        userDeviceDao.recordDevice(user.getId(), ipAddress, userAgent, deviceType, deviceName);
        return user;
    }

    @Override
    public User updateUser(
            final BigInteger userId,
            final String username,
            final String avatarUrl,
            final String bio,
            final Boolean needFriendVerify
    ) {
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setAvatarUrl(avatarUrl);
        user.setBio(bio);
        user.setNeedFriendVerify(needFriendVerify);
        userDao.updateUser(user);
        return userDao.getById(userId);
    }

    /**
     * 修改当前用户密码.
     *
     * @param userId          用户ID
     * @param currentPassword 当前密码
     * @param newPassword     新密码
     */
    @Override
    public void resetPassword(
            final BigInteger userId,
            final String currentPassword,
            final String newPassword
    ) {
        User user = userDao.getById(userId);
        AssertUtils.nonNull(user, BizErrorEnum.USER_NOT_FOUND);

        // 解密存储密码并校验当前密码，校验通过后再加密新密码。
        String decryptedPassword = CryptoUtils.decrypt(user.getPassword());
        AssertUtils.equal(currentPassword, decryptedPassword, BizErrorEnum.CURRENT_PASSWORD_ERROR);
        user.changePassword(CryptoUtils.encrypt(newPassword));
        userDao.updatePassword(user);
    }

    @Override
    public User getUserById(final BigInteger userId) {
        User user = userDao.getById(userId);
        AssertUtils.nonNull(user, BizErrorEnum.USER_NOT_FOUND);
        return user;
    }

    @Override
    public User getUserByCode(final String code) {
        User user = userDao.getByCode(code);
        AssertUtils.nonNull(user, BizErrorEnum.USER_NOT_FOUND);
        return user;
    }

    /**
     * 上传用户头像.
     *
     * @param userId 用户ID
     * @param file   头像文件
     * @return 头像地址
     */
    @Override
    public String uploadAvatar(final BigInteger userId, final MultipartFile file) {
        return fileGrpcClient.uploadFile(userId, file);
    }
}
