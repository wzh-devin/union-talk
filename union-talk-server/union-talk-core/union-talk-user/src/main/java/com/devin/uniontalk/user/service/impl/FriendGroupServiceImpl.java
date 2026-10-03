package com.devin.uniontalk.user.service.impl;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.user.dao.FriendGroupDao;
import com.devin.uniontalk.user.domain.entity.FriendGroup;
import com.devin.uniontalk.user.service.FriendGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 好友分组(FriendGroup)ServiceImpl层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendGroupServiceImpl implements FriendGroupService {

    private final FriendGroupDao friendGroupDao;

    @Override
    public FriendGroup createFriendGroup(
            final BigInteger userId,
            final String name,
            final Integer sortOrder
    ) {
        // 检查同名分组是否已存在
        FriendGroup existing = friendGroupDao.getByUserIdAndName(userId, name);
        AssertUtils.isNull(existing, BizErrorEnum.DUPLICATE_ENTITY);

        // 构建分组实体并持久化
        FriendGroup friendGroup = new FriendGroup();
        friendGroup.setId(IdGenerator.nextIdBigInteger());
        friendGroup.setUserId(userId);
        friendGroup.setName(name);
        friendGroup.setSortOrder(Objects.nonNull(sortOrder) ? sortOrder : 0);
        friendGroup.init();
        friendGroupDao.save(friendGroup);
        return friendGroup;
    }

    @Override
    public List<FriendGroup> getFriendGroupList(final BigInteger userId) {
        return friendGroupDao.listByUserId(userId);
    }

    @Override
    public void updateFriendGroup(
            final BigInteger userId,
            final BigInteger groupId,
            final String name,
            final Integer sortOrder
    ) {
        // 校验分组存在且归属当前用户
        FriendGroup friendGroup = friendGroupDao.getById(groupId);
        AssertUtils.nonNull(friendGroup, BizErrorEnum.FRIEND_GROUP_NOT_FOUND);
        AssertUtils.equal(friendGroup.getUserId(), userId, BizErrorEnum.FRIEND_GROUP_NOT_FOUND);

        // 构建实体，条件更新非空字段
        FriendGroup updateEntity = new FriendGroup();
        updateEntity.setId(groupId);
        updateEntity.setName(name);
        updateEntity.setSortOrder(sortOrder);
        friendGroupDao.updateFriendGroup(updateEntity);
    }

    @Override
    public void deleteFriendGroup(final BigInteger userId, final BigInteger groupId) {
        // 校验分组存在且归属当前用户
        FriendGroup friendGroup = friendGroupDao.getById(groupId);
        AssertUtils.nonNull(friendGroup, BizErrorEnum.FRIEND_GROUP_NOT_FOUND);
        AssertUtils.equal(friendGroup.getUserId(), userId, BizErrorEnum.FRIEND_GROUP_NOT_FOUND);

        friendGroupDao.removeById(groupId);
    }
}
