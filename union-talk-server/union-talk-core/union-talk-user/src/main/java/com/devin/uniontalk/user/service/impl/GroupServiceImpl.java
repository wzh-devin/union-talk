package com.devin.uniontalk.user.service.impl;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.infrastructure.user.enums.GroupMemberRoleEnum;
import com.devin.uniontalk.infrastructure.user.enums.GroupStatusEnum;
import com.devin.uniontalk.user.dao.GroupDao;
import com.devin.uniontalk.user.dao.GroupMemberDao;
import com.devin.uniontalk.user.dao.UserRelationDao;
import com.devin.uniontalk.user.domain.entity.Group;
import com.devin.uniontalk.user.domain.entity.GroupMember;
import com.devin.uniontalk.user.domain.entity.UserRelation;
import com.devin.uniontalk.user.domain.entity.convertor.GroupConvertor;
import com.devin.uniontalk.user.domain.vo.resp.GroupRespVO;
import com.devin.uniontalk.user.grpc.client.MessageConversationGrpcClient;
import com.devin.uniontalk.user.service.GroupService;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
 * 群聊(Group)ServiceImpl层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    /**
     * 默认群成员人数限制.
     */
    private static final Integer DEFAULT_GROUP_MEMBER_LIMIT = 100;

    /**
     * 最大群成员人数限制.
     */
    private static final Integer MAX_GROUP_MEMBER_LIMIT = 100;

    /**
     * 最小群成员人数限制.
     */
    private static final Integer MIN_GROUP_MEMBER_COUNT = 3;

    /**
     * 群聊 Dao.
     */
    private final GroupDao groupDao;

    /**
     * 群成员 Dao.
     */
    private final GroupMemberDao groupMemberDao;

    /**
     * 用户关系 Dao.
     */
    private final UserRelationDao userRelationDao;

    /**
     * Message会话Grpc客户端.
     */
    private final MessageConversationGrpcClient messageConversationGrpcClient;

    @Override
    @GlobalTransactional(name = "group-create", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public GroupRespVO createGroup(
            final BigInteger ownerId,
            final String name,
            final String avatarUrl,
            final String description,
            final Integer memberLimit,
            final List<BigInteger> userIdList
    ) {
        List<BigInteger> memberIdList = getMemberIdList(ownerId, userIdList);
        int groupMemberLimit = Objects.nonNull(memberLimit) ? memberLimit : DEFAULT_GROUP_MEMBER_LIMIT;
        AssertUtils.isTrue(groupMemberLimit >= MIN_GROUP_MEMBER_COUNT, BizErrorEnum.GROUP_MEMBER_MIN_LIMIT);
        AssertUtils.isTrue(groupMemberLimit <= MAX_GROUP_MEMBER_LIMIT, BizErrorEnum.GROUP_MEMBER_LIMIT);
        AssertUtils.isTrue(
                memberIdList.size() + 1 >= MIN_GROUP_MEMBER_COUNT,
                BizErrorEnum.GROUP_MEMBER_MIN_LIMIT
        );
        AssertUtils.isTrue(
                memberIdList.size() + 1 <= groupMemberLimit,
                BizErrorEnum.GROUP_MEMBER_LIMIT
        );
        validateFriendRelations(ownerId, memberIdList);

        // 创建群聊实体
        Group group = new Group();
        group.setId(IdGenerator.nextIdBigInteger());
        group.setOwnerId(ownerId);
        group.setName(name);
        group.setAvatarUrl(Objects.nonNull(avatarUrl) ? avatarUrl : "");
        group.setDescription(Objects.nonNull(description) ? description : "");
        group.setMemberLimit(groupMemberLimit);
        group.setStatus(GroupStatusEnum.ACTIVE.name());
        group.init();
        groupDao.save(group);

        // 批量写入群主与选中的好友成员
        List<GroupMember> groupMemberList = getGroupMemberList(group.getId(), ownerId, memberIdList);
        groupMemberDao.saveBatch(groupMemberList);

        // 同步通知 message 模块创建群聊会话，失败时回滚群聊与群成员数据。
        messageConversationGrpcClient.createGroupConversation(
                group.getId(),
                groupMemberList.stream()
                        .map(GroupMember::getUserId)
                        .toList()
        );

        return toRespVO(group);
    }

    @Override
    public void updateGroup(
            final BigInteger userId,
            final BigInteger groupId,
            final String name,
            final String avatarUrl,
            final String description,
            final Integer memberLimit
    ) {
        // 校验群聊存在且操作人为群主
        getAndValidateOwner(userId, groupId);
        // 构建实体，条件更新非空字段
        Group group = new Group();
        group.setId(groupId);
        group.setName(name);
        group.setAvatarUrl(avatarUrl);
        group.setDescription(description);
        group.setMemberLimit(memberLimit);
        AssertUtils.isTrue(
                Objects.isNull(memberLimit) || memberLimit >= MIN_GROUP_MEMBER_COUNT,
                BizErrorEnum.GROUP_MEMBER_MIN_LIMIT
        );
        AssertUtils.isTrue(
                Objects.isNull(memberLimit) || memberLimit <= MAX_GROUP_MEMBER_LIMIT,
                BizErrorEnum.GROUP_MEMBER_LIMIT
        );
        groupDao.updateGroup(group);
    }

    @Override
    @GlobalTransactional(name = "group-dissolve", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(final BigInteger userId, final BigInteger groupId) {
        // 校验群聊存在且操作人为群主
        Group group = getAndValidateOwner(userId, groupId);

        // 标记群聊为已解散
        group.setStatus(GroupStatusEnum.DISSOLVED.name());
        group.setDissolvedAt(new Date().toString());
        group.setUpdatedAt(new Date());
        groupDao.updateById(group);

        // 同步通知 message 模块解散关联会话，失败时回滚群聊状态更新。
        messageConversationGrpcClient.dissolveGroupConversation(groupId);
    }

    @Override
    public GroupRespVO getGroupDetail(final BigInteger groupId) {
        Group group = groupDao.getById(groupId);
        AssertUtils.nonNull(group, BizErrorEnum.GROUP_NOT_FOUND);
        return toRespVO(group);
    }

    /**
     * 校验群聊存在且操作人为群主.
     *
     * @param uid     用户id
     * @param groupId 群聊id
     * @return 群聊实体
     */
    private Group getAndValidateOwner(final BigInteger uid, final BigInteger groupId) {
        Group group = groupDao.getById(groupId);
        AssertUtils.nonNull(group, BizErrorEnum.GROUP_NOT_FOUND);
        AssertUtils.equal(group.getOwnerId(), uid, BizErrorEnum.NOT_GROUP_OWNER);
        return group;
    }

    /**
     * 实体转响应VO并补充成员数量.
     *
     * @param group 群聊实体
     * @return 响应VO
     */
    private GroupRespVO toRespVO(final Group group) {
        GroupRespVO respVO = GroupConvertor.INSTANCE.toRespVO(group);
        respVO.setMemberCount(groupMemberDao.countByGroupId(group.getId()));
        return respVO;
    }

    /**
     * 获取群成员id列表.
     *
     * @param ownerId    群主id
     * @param userIdList 原始群成员id列表
     * @return 群成员id列表
     */
    private List<BigInteger> getMemberIdList(final BigInteger ownerId, final List<BigInteger> userIdList) {
        if (Objects.isNull(userIdList) || userIdList.isEmpty()) {
            return List.of();
        }
        return userIdList.stream()
                .filter(Objects::nonNull)
                .filter(userId -> !ownerId.equals(userId))
                .distinct()
                .toList();
    }

    /**
     * 校验群成员均为群主好友.
     *
     * @param ownerId      群主id
     * @param memberIdList 群成员id列表
     */
    private void validateFriendRelations(final BigInteger ownerId, final List<BigInteger> memberIdList) {
        if (memberIdList.isEmpty()) {
            return;
        }
        List<UserRelation> relationList = userRelationDao.getFriendRelationList(ownerId, memberIdList);
        Set<BigInteger> friendIdSet = relationList.stream()
                .map(UserRelation::getTargetId)
                .collect(Collectors.toSet());
        AssertUtils.isTrue(friendIdSet.containsAll(memberIdList), BizErrorEnum.USER_NOT_FOUND);
    }

    /**
     * 构建群成员列表.
     *
     * @param groupId      群聊id
     * @param ownerId      群主id
     * @param memberIdList 群成员id列表
     * @return 群成员列表
     */
    private List<GroupMember> getGroupMemberList(
            final BigInteger groupId,
            final BigInteger ownerId,
            final List<BigInteger> memberIdList
    ) {
        List<GroupMember> groupMemberList = memberIdList.stream()
                .map(userId -> buildGroupMember(groupId, userId, GroupMemberRoleEnum.MEMBER))
                .collect(Collectors.toList());
        groupMemberList.addFirst(buildGroupMember(groupId, ownerId, GroupMemberRoleEnum.OWNER));
        return groupMemberList;
    }

    /**
     * 构建群成员实体.
     *
     * @param groupId 群聊id
     * @param userId  用户id
     * @param role    群成员角色
     * @return 群成员实体
     */
    private GroupMember buildGroupMember(
            final BigInteger groupId,
            final BigInteger userId,
            final GroupMemberRoleEnum role
    ) {
        GroupMember member = new GroupMember();
        member.setId(IdGenerator.nextIdBigInteger());
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role.name());
        member.setNickname("");
        member.setJoinedAt(new Date());
        member.init();
        return member;
    }
}
