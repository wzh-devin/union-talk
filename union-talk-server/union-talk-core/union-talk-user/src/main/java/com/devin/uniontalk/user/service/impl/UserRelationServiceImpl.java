package com.devin.uniontalk.user.service.impl;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.infrastructure.user.enums.UserRelationStatusEnum;
import com.devin.uniontalk.user.dao.UserDao;
import com.devin.uniontalk.user.dao.UserRelationDao;
import com.devin.uniontalk.user.domain.entity.User;
import com.devin.uniontalk.user.domain.entity.UserRelation;
import com.devin.uniontalk.user.domain.entity.convertor.UserRelationConvertor;
import com.devin.uniontalk.user.domain.vo.resp.FriendRespVO;
import com.devin.uniontalk.user.service.UserRelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 用户关系(UserRelation)ServiceImpl层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRelationServiceImpl implements UserRelationService {

    /**
     * 用户关系 Dao.
     */
    private final UserRelationDao userRelationDao;

    /**
     * 用户 Dao.
     */
    private final UserDao userDao;

    @Override
    public List<FriendRespVO> getFriendList(final BigInteger userId) {
        List<UserRelation> relationList = userRelationDao.getFriendList(userId);
        if (relationList.isEmpty()) {
            return List.of();
        }
        List<BigInteger> targetIdList = relationList.stream()
                .map(UserRelation::getTargetId)
                .distinct()
                .toList();
        Map<BigInteger, User> userMap = userDao.listByIds(targetIdList).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return relationList.stream().map(relation -> {
            FriendRespVO respVO = UserRelationConvertor.INSTANCE.toRespVO(relation);
            User targetUser = userMap.get(relation.getTargetId());
            if (Objects.nonNull(targetUser)) {
                respVO.setCode(targetUser.getCode());
                respVO.setUsername(targetUser.getUsername());
                respVO.setAvatarUrl(targetUser.getAvatarUrl());
            }
            return respVO;
        }).toList();
    }

    @Override
    public void updateRemark(final BigInteger userId, final BigInteger targetId, final String remark) {
        UserRelation relation = getValidRelation(userId, targetId);
        relation.setRemark(remark);
        relation.setUpdatedAt(new Date());
        userRelationDao.updateById(relation);
    }

    @Override
    public void moveFriendGroup(final BigInteger userId, final BigInteger targetId, final BigInteger friendGroupId) {
        UserRelation relation = getValidRelation(userId, targetId);
        relation.setFriendGroupId(friendGroupId);
        relation.setUpdatedAt(new Date());
        userRelationDao.updateById(relation);
    }

    @Override
    public void deleteFriend(final BigInteger userId, final BigInteger targetId) {
        userRelationDao.updateStatusBidirectional(userId, targetId, UserRelationStatusEnum.DELETED);
    }

    @Override
    public void blockFriend(final BigInteger userId, final BigInteger targetId) {
        userRelationDao.updateStatus(userId, targetId, UserRelationStatusEnum.BLOCKED);
    }

    @Override
    public void unblockFriend(final BigInteger userId, final BigInteger targetId) {
        UserRelation relation = userRelationDao.getRelation(userId, targetId);
        AssertUtils.nonNull(relation, BizErrorEnum.USER_NOT_FOUND);
        relation.setStatus(UserRelationStatusEnum.FRIEND.name());
        relation.setBlockedAt(null);
        relation.setUpdatedAt(new Date());
        userRelationDao.updateById(relation);
    }

    /**
     * 校验好友关系存在且状态为FRIEND.
     * @param uid 用户id
     * @param targetUid 目标用户id
     * @return UserRelation
     */
    private UserRelation getValidRelation(final BigInteger uid, final BigInteger targetUid) {
        UserRelation relation = userRelationDao.getRelation(uid, targetUid);
        AssertUtils.nonNull(relation, BizErrorEnum.USER_NOT_FOUND);
        AssertUtils.equal(relation.getStatus(), UserRelationStatusEnum.FRIEND.name(), BizErrorEnum.USER_NOT_FOUND);
        return relation;
    }
}
