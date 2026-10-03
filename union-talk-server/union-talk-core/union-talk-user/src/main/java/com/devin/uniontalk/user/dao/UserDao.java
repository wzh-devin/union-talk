package com.devin.uniontalk.user.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.infrastructure.user.enums.UserStatusEnum;
import com.devin.uniontalk.user.domain.entity.User;
import com.devin.uniontalk.user.mapper.UserMapper;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 2026/05/12 22:01:37.
 *
 * <p>
 *  用户表(User)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDao extends ServiceImpl<UserMapper, User> {

    /**
     * 检查用户名或邮箱是否已存在.
     *
     * @param username 用户名
     * @param email    邮箱
     * @return true表示用户已存在
     */
    public Boolean checkUserExist(final String username, final String email) {
        boolean hasUsername = StringUtils.hasLength(username);
        boolean hasEmail = StringUtils.hasLength(email);
        if (!hasUsername && !hasEmail) {
            return Boolean.FALSE;
        }
        return lambdaQuery()
                .eq(hasUsername, User::getUsername, username)
                .or(hasUsername && hasEmail)
                .eq(hasEmail, User::getEmail, email)
                .exists();
    }

    /**
     * 根据邮箱查询用户.
     *
     * @param email 邮箱
     * @return 用户实体，不存在则返回null
     */
    public User getByEmail(final String email) {
        return lambdaQuery()
                .eq(User::getEmail, email)
                .one();
    }

    /**
     * 根据账号查询用户（支持用户名或邮箱匹配）.
     *
     * @param account 登录账号（用户名或邮箱）
     * @return 用户实体，不存在则返回null
     */
    public User getByAccount(final String account) {
        return lambdaQuery()
                .eq(User::getUsername, account)
                .or()
                .eq(User::getEmail, account)
                .one();
    }

    /**
     * 根据用户唯一CODE查询用户.
     *
     * @param code 用户唯一CODE
     * @return 用户实体，不存在则返回null
     */
    public User getByCode(final String code) {
        return lambdaQuery()
                .eq(User::getStatus, UserStatusEnum.NORMAL.name())
                .eq(User::getCode, code)
                .one();
    }

    /**
     * 批量查询用户列表.
     *
     * @param idList 用户id列表
     * @return 用户列表
     */
    public List<User> getUserListByIds(final List<BigInteger> idList) {
        if (Objects.isNull(idList) || idList.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(User::getId, idList)
                .list();
    }

    /**
     * 条件更新用户信息，仅更新非空字段.
     *
     * @param user 包含待更新字段的用户实体（id 必填）
     */
    public void updateUser(final User user) {
        lambdaUpdate()
                .set(StringUtils.hasLength(user.getUsername()), User::getUsername, user.getUsername())
                .set(StringUtils.hasLength(user.getAvatarUrl()), User::getAvatarUrl, user.getAvatarUrl())
                .set(StringUtils.hasLength(user.getBio()), User::getBio, user.getBio())
                .set(Objects.nonNull(user.getNeedFriendVerify()), User::getNeedFriendVerify, user.getNeedFriendVerify())
                .set(User::getUpdatedAt, new Date())
                .eq(User::getId, user.getId())
                .update();
    }

    /**
     * 更新用户密码和更新时间.
     *
     * @param user 包含用户ID、加密密码和更新时间的用户实体
     */
    public void updatePassword(final User user) {
        lambdaUpdate()
                .set(User::getPassword, user.getPassword())
                .set(User::getUpdatedAt, user.getUpdatedAt())
                .eq(User::getId, user.getId())
                .update();
    }
}
