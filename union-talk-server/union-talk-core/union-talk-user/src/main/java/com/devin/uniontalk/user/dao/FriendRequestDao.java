package com.devin.uniontalk.user.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.infrastructure.user.enums.FriendRequestStatusEnum;
import com.devin.uniontalk.user.domain.entity.FriendRequest;
import com.devin.uniontalk.user.mapper.FriendRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigInteger;
import java.util.List;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 好友申请(FriendRequest)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRequestDao extends ServiceImpl<FriendRequestMapper, FriendRequest> {

    /**
     * 查询两人之间待处理的好友申请.
     *
     * @param fromUserId 申请人id
     * @param toUserId   目标用户id
     * @return 待处理的好友申请，不存在则返回null
     */
    public FriendRequest getPendingRequest(final BigInteger fromUserId, final BigInteger toUserId) {
        return lambdaQuery()
                .eq(FriendRequest::getFromUserId, fromUserId)
                .eq(FriendRequest::getToUserId, toUserId)
                .eq(FriendRequest::getStatus, FriendRequestStatusEnum.PENDING.name())
                .one();
    }

    /**
     * 查询用户收到的好友申请列表.
     *
     * @param toUserId 目标用户id
     * @return 好友申请列表
     */
    public List<FriendRequest> listByToUserId(final BigInteger toUserId) {
        return lambdaQuery()
                .eq(FriendRequest::getToUserId, toUserId)
                .orderByDesc(FriendRequest::getCreatedAt)
                .list();
    }

    /**
     * 查询用户发出的好友申请列表.
     *
     * @param fromUserId 申请人id
     * @return 好友申请列表
     */
    public List<FriendRequest> listByFromUserId(final BigInteger fromUserId) {
        return lambdaQuery()
                .eq(FriendRequest::getFromUserId, fromUserId)
                .orderByDesc(FriendRequest::getCreatedAt)
                .list();
    }
}
