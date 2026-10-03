package com.devin.uniontalk.user.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.infrastructure.user.enums.UserRelationStatusEnum;
import com.devin.uniontalk.user.domain.entity.UserRelation;
import com.devin.uniontalk.user.mapper.UserRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 用户关系(UserRelation)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRelationDao extends ServiceImpl<UserRelationMapper, UserRelation> {

    /**
     * 查询两人之间的关系记录.
     *
     * @param userId   用户id
     * @param targetId 对方id
     * @return 关系记录，不存在则返回null
     */
    public UserRelation getRelation(final BigInteger userId, final BigInteger targetId) {
        return lambdaQuery()
                .eq(UserRelation::getUserId, userId)
                .eq(UserRelation::getTargetId, targetId)
                .one();
    }

    /**
     * 查询用户的好友列表（状态为FRIEND）.
     *
     * @param userId 用户id
     * @return 好友关系列表
     */
    public List<UserRelation> getFriendList(final BigInteger userId) {
        return lambdaQuery()
                .eq(UserRelation::getUserId, userId)
                .eq(UserRelation::getStatus, UserRelationStatusEnum.FRIEND.name())
                .list();
    }

    /**
     * 批量查询用户与目标用户之间的好友关系.
     *
     * @param userId       用户id
     * @param targetIdList 目标用户id列表
     * @return 好友关系列表
     */
    public List<UserRelation> getFriendRelationList(final BigInteger userId, final List<BigInteger> targetIdList) {
        return lambdaQuery()
                .eq(UserRelation::getUserId, userId)
                .in(UserRelation::getTargetId, targetIdList)
                .eq(UserRelation::getStatus, UserRelationStatusEnum.FRIEND.name())
                .list();
    }

    /**
     * 按分组查询好友关系列表.
     *
     * @param friendGroupId 好友分组id
     * @return 好友关系列表
     */
    public List<UserRelation> getFriendListByFriendGroupId(final BigInteger friendGroupId) {
        return lambdaQuery()
                .eq(UserRelation::getFriendGroupId, friendGroupId)
                .eq(UserRelation::getStatus, UserRelationStatusEnum.FRIEND.name())
                .list();
    }

    /**
     * 更新关系状态及时间戳.
     *
     * @param userId   用户id
     * @param targetId 对方id
     * @param status   新状态
     */
    public void updateStatus(
            final BigInteger userId,
            final BigInteger targetId,
            final UserRelationStatusEnum status
    ) {
        lambdaUpdate()
                .set(UserRelation::getStatus, status.name())
                .set(UserRelationStatusEnum.BLOCKED == status, UserRelation::getBlockedAt, new Date())
                .set(UserRelationStatusEnum.DELETED == status, UserRelation::getDeletedAt, new Date())
                .set(UserRelation::getUpdatedAt, new Date())
                .eq(UserRelation::getUserId, userId)
                .eq(UserRelation::getTargetId, targetId)
                .update();
    }

    /**
     * 双向更新关系状态（一次SQL完成双向更新）.
     *
     * @param userId   用户id
     * @param targetId 对方id
     * @param status   新状态
     */
    public void updateStatusBidirectional(
            final BigInteger userId,
            final BigInteger targetId,
            final UserRelationStatusEnum status
    ) {
        lambdaUpdate()
                .set(UserRelation::getStatus, status.name())
                .set(UserRelationStatusEnum.BLOCKED == status, UserRelation::getBlockedAt, new Date())
                .set(UserRelationStatusEnum.DELETED == status, UserRelation::getDeletedAt, new Date())
                .set(UserRelation::getUpdatedAt, new Date())
                .and(w -> w
                        .nested(n -> n.eq(UserRelation::getUserId, userId).eq(UserRelation::getTargetId, targetId))
                        .or()
                        .nested(n -> n.eq(UserRelation::getUserId, targetId).eq(UserRelation::getTargetId, userId))
                )
                .update();
    }

}
