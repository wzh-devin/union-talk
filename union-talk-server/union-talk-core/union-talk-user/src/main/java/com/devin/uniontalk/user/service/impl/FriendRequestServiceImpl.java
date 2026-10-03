package com.devin.uniontalk.user.service.impl;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.infrastructure.user.enums.FriendRequestStatusEnum;
import com.devin.uniontalk.infrastructure.user.enums.UserRelationStatusEnum;
import com.devin.uniontalk.user.dao.FriendGroupDao;
import com.devin.uniontalk.user.dao.FriendRequestDao;
import com.devin.uniontalk.user.dao.UserDao;
import com.devin.uniontalk.user.dao.UserRelationDao;
import com.devin.uniontalk.user.domain.entity.FriendGroup;
import com.devin.uniontalk.user.domain.entity.FriendRequest;
import com.devin.uniontalk.user.domain.entity.User;
import com.devin.uniontalk.user.domain.entity.UserRelation;
import com.devin.uniontalk.user.domain.entity.convertor.FriendRequestConvertor;
import com.devin.uniontalk.user.domain.vo.resp.FriendRequestRespVO;
import com.devin.uniontalk.user.grpc.client.MessageConversationGrpcClient;
import com.devin.uniontalk.user.mq.publisher.FriendRequestAcceptedMqPublisher;
import com.devin.uniontalk.user.mq.publisher.FriendRequestMqPublisher;
import com.devin.uniontalk.user.service.FriendRequestService;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 好友申请(FriendRequest)ServiceImpl层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRequestServiceImpl implements FriendRequestService {

    /**
     * 好友申请 Dao.
     */
    private final FriendRequestDao friendRequestDao;

    /**
     * 用户关系 Dao.
     */
    private final UserRelationDao userRelationDao;

    /**
     * 用户 Dao.
     */
    private final UserDao userDao;

    /**
     * 好友分组 Dao.
     */
    private final FriendGroupDao friendGroupDao;

    /**
     * 好友申请 MQ 发布器.
     */
    private final FriendRequestMqPublisher friendRequestMqPublisher;

    /**
     * 好友申请同意 MQ 发布器.
     */
    private final FriendRequestAcceptedMqPublisher friendRequestAcceptedMqPublisher;

    /**
     * Message会话Grpc客户端.
     */
    private final MessageConversationGrpcClient messageConversationGrpcClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendRequest(
            final BigInteger fromUserId,
            final String toUserCode,
            final String applyMsg
    ) {
        // 根据code查询目标用户
        User toUser = userDao.getByCode(toUserCode);
        AssertUtils.nonNull(toUser, BizErrorEnum.USER_NOT_FOUND);
        BigInteger toUserId = toUser.getId();

        // 检查是否已经是好友
        UserRelation relation = userRelationDao.getRelation(fromUserId, toUserId);
        AssertUtils.isFalse(
                Objects.nonNull(relation) && UserRelationStatusEnum.FRIEND.name().equals(relation.getStatus()),
                BizErrorEnum.ALREADY_FRIENDS
        );

        // 检查是否已有待处理的申请
        FriendRequest pending = friendRequestDao.getPendingRequest(fromUserId, toUserId);
        AssertUtils.isNull(pending, BizErrorEnum.FRIEND_REQUEST_ALREADY_SENT);

        // 查询申请人信息，用于好友申请实时通知
        User fromUser = userDao.getById(fromUserId);

        // 创建好友申请记录
        FriendRequest request = new FriendRequest();
        request.setId(IdGenerator.nextIdBigInteger());
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setApplyMsg(applyMsg);
        request.setStatus(FriendRequestStatusEnum.PENDING.name());
        request.init();
        friendRequestDao.save(request);
        friendRequestMqPublisher.publishCreated(request, fromUser);
    }

    @Override
    @GlobalTransactional(name = "friend-request-handle", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public void handleRequest(
            final BigInteger userId,
            final BigInteger requestId,
            final Boolean accept,
            final BigInteger friendGroupId,
            final String remark
    ) {
        BigInteger groupId = friendGroupId;
        // 校验申请记录存在且属于当前用户
        FriendRequest request = friendRequestDao.getById(requestId);
        AssertUtils.nonNull(request, BizErrorEnum.FRIEND_REQUEST_NOT_FOUND);
        AssertUtils.equal(request.getToUserId(), userId, BizErrorEnum.FRIEND_REQUEST_NOT_FOUND);

        // 更新申请状态
        String newStatus = accept ? FriendRequestStatusEnum.ACCEPTED.name() : FriendRequestStatusEnum.REJECTED.name();
        request.setStatus(newStatus);
        request.setHandledAt(new Date());
        request.setUpdatedAt(new Date());
        friendRequestDao.updateById(request);

        // 判断是否明确指定用户分组
        if (Objects.isNull(groupId)) {
            FriendGroup defaultGroup = friendGroupDao.getByUserIdAndName(userId, "默认分组");
            if (Objects.isNull(defaultGroup)) {
                defaultGroup = new FriendGroup();
                defaultGroup.setId(IdGenerator.nextIdBigInteger());
                defaultGroup.setUserId(userId);
                defaultGroup.setName("默认分组");
                defaultGroup.init();
                friendGroupDao.save(defaultGroup);
            }
            groupId = defaultGroup.getId();
        }

        // 接受申请时，双向创建好友关系
        if (accept) {
            User toUser = userDao.getById(userId);
            AssertUtils.nonNull(toUser, BizErrorEnum.USER_NOT_FOUND);
            createBidirectionalRelation(request.getFromUserId(), userId, groupId, remark);
            BigInteger conversationId = messageConversationGrpcClient.createPrivateConversation(
                    request.getFromUserId(),
                    userId,
                    request.getId()
            );
            publishAccepted(request, toUser, conversationId);
        }
    }

    @Override
    public List<FriendRequestRespVO> getReceivedRequestList(final BigInteger userId) {
        return batchConvert(friendRequestDao.listByToUserId(userId));
    }

    @Override
    public List<FriendRequestRespVO> getSentRequestList(final BigInteger userId) {
        return batchConvert(friendRequestDao.listByFromUserId(userId));
    }

    /**
     * 批量转换好友申请列表，一次性查询所有申请人信息.
     * @param requestList 好友申请列表
     * @return 转换后的好友申请列表
     */
    private List<FriendRequestRespVO> batchConvert(final List<FriendRequest> requestList) {
        if (requestList.isEmpty()) {
            return List.of();
        }
        List<BigInteger> fromUserIdList = requestList.stream()
                .map(FriendRequest::getFromUserId)
                .distinct()
                .toList();
        Map<BigInteger, User> userMap = userDao.listByIds(fromUserIdList).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return requestList.stream().map(request -> {
            FriendRequestRespVO respVO = FriendRequestConvertor.INSTANCE.toRespVO(request);
            User fromUser = userMap.get(request.getFromUserId());
            if (Objects.nonNull(fromUser)) {
                respVO.setFromUsername(fromUser.getUsername());
                respVO.setFromAvatarUrl(fromUser.getAvatarUrl());
            }
            return respVO;
        }).toList();
    }

    /**
     * 双向创建好友关系记录.
     *
     * @param fromUserId    申请方ID
     * @param toUserId      接受方ID
     * @param friendGroupId 好友分组ID
     * @param remark        备注
     */
    private void createBidirectionalRelation(
            final BigInteger fromUserId,
            final BigInteger toUserId,
            final BigInteger friendGroupId,
            final String remark
    ) {
        // 查询双方用户信息
        Map<BigInteger, User> userMap = userDao.listByIds(List.of(fromUserId, toUserId))
                .stream()
                .collect(
                        Collectors.toMap(User::getId, Function.identity())
                );
        User fromUser = userMap.get(fromUserId);
        User toUser = userMap.get(toUserId);

        // 为接受方创建关系（接受方视角：对方是申请方）
        UserRelation toRelation = new UserRelation();
        toRelation.setId(IdGenerator.nextIdBigInteger());
        toRelation.setUserId(toUserId);
        toRelation.setTargetId(fromUserId);
        toRelation.setStatus(UserRelationStatusEnum.FRIEND.name());
        toRelation.setRemark(Objects.nonNull(remark) ? remark : (Objects.nonNull(fromUser) ? fromUser.getUsername() : ""));
        toRelation.setFriendGroupId(friendGroupId);
        toRelation.init();

        // 为申请方创建关系（申请方视角：对方是接受方）
        UserRelation fromRelation = new UserRelation();
        fromRelation.setId(IdGenerator.nextIdBigInteger());
        fromRelation.setUserId(fromUserId);
        fromRelation.setTargetId(toUserId);
        fromRelation.setStatus(UserRelationStatusEnum.FRIEND.name());
        fromRelation.setRemark(Objects.nonNull(toUser) ? toUser.getUsername() : "");
        fromRelation.setFriendGroupId(BigInteger.ZERO);
        fromRelation.init();

        // 一次批量保存
        userRelationDao.saveBatch(List.of(toRelation, fromRelation));
    }

    /**
     * 发布好友申请同意事件.
     *
     * @param request        好友申请记录
     * @param toUser         同意用户
     * @param conversationId 会话id
     */
    private void publishAccepted(
            final FriendRequest request,
            final User toUser,
            final BigInteger conversationId
    ) {
        try {
            friendRequestAcceptedMqPublisher.publishAccepted(request, toUser, conversationId);
        } catch (Exception e) {
            log.error("发送好友申请同意MQ事件失败, friendRequestId={}", request.getId(), e);
        }
    }
}
