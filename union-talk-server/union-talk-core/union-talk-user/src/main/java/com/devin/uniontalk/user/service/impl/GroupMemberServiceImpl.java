package com.devin.uniontalk.user.service.impl;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.infrastructure.user.enums.GroupMemberRoleEnum;
import com.devin.uniontalk.infrastructure.user.enums.GroupStatusEnum;
import com.devin.uniontalk.user.dao.GroupDao;
import com.devin.uniontalk.user.dao.GroupMemberDao;
import com.devin.uniontalk.user.dao.UserDao;
import com.devin.uniontalk.user.domain.entity.Group;
import com.devin.uniontalk.user.domain.entity.GroupMember;
import com.devin.uniontalk.user.domain.entity.User;
import com.devin.uniontalk.user.domain.entity.convertor.GroupMemberConvertor;
import com.devin.uniontalk.user.domain.vo.resp.GroupMemberRespVO;
import com.devin.uniontalk.user.service.GroupMemberService;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 群成员(GroupMember)ServiceImpl层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMemberServiceImpl implements GroupMemberService {

    /**
     * 群聊 Dao.
     */
    private final GroupDao groupDao;

    /**
     * 群成员 Dao.
     */
    private final GroupMemberDao groupMemberDao;

    /**
     * 用户 Dao.
     */
    private final UserDao userDao;

    /**
     * 邀请用户入群.
     *
     * @param userId     操作用户id
     * @param groupId    群聊id
     * @param userIdList 被邀请用户id列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteMembers(
            final BigInteger userId,
            final BigInteger groupId,
            final List<BigInteger> userIdList
    ) {
        // 校验群聊存在
        Group group = groupDao.getById(groupId);
        AssertUtils.nonNull(group, BizErrorEnum.GROUP_NOT_FOUND);

        // 校验成员数量是否超限
        long currentCount = groupMemberDao.countByGroupId(groupId);
        AssertUtils.isTrue(
                currentCount + userIdList.size() <= group.getMemberLimit(),
                BizErrorEnum.GROUP_MEMBER_LIMIT
        );

        // 批量查询已在群中的用户，过滤后批量插入
        List<GroupMember> existingMemberList = groupMemberDao.getGroupMemberListByGroupId(groupId);
        Set<BigInteger> existingUserIdSet = existingMemberList.stream()
                .map(GroupMember::getUserId)
                .collect(Collectors.toSet());

        List<GroupMember> newMemberList = userIdList.stream()
                .filter(uid -> !existingUserIdSet.contains(uid))
                .map(uid -> {
                    GroupMember member = new GroupMember();
                    member.setId(IdGenerator.nextIdBigInteger());
                    member.setGroupId(groupId);
                    member.setUserId(uid);
                    member.setRole(GroupMemberRoleEnum.MEMBER.name());
                    member.setNickname("");
                    member.setJoinedAt(new Date());
                    member.init();
                    return member;
                })
                .toList();

        if (!newMemberList.isEmpty()) {
            groupMemberDao.saveBatch(newMemberList);
        }
    }

    /**
     * 退出群聊.
     *
     * @param userId  用户id
     * @param groupId 群聊id
     */
    @Override
    public void leaveGroup(final BigInteger userId, final BigInteger groupId) {
        GroupMember member = groupMemberDao.getByGroupAndUser(groupId, userId);
        AssertUtils.nonNull(member, BizErrorEnum.GROUP_NOT_FOUND);
        // 群主不能退出，需先转让或解散
        AssertUtils.isFalse(
                GroupMemberRoleEnum.OWNER.name().equals(member.getRole()),
                BizErrorEnum.NOT_GROUP_OWNER
        );
        member.setLeftAt(new Date());
        member.setUpdatedAt(new Date());
        groupMemberDao.updateById(member);
    }

    /**
     * 踢出群成员.
     *
     * @param operatorId 操作人id
     * @param groupId    群聊id
     * @param targetId   被踢用户id
     */
    @Override
    public void kickMember(
            final BigInteger operatorId,
            final BigInteger groupId,
            final BigInteger targetId
    ) {
        // 校验操作人为群主或管理员
        GroupMember operator = groupMemberDao.getByGroupAndUser(groupId, operatorId);
        AssertUtils.nonNull(operator, BizErrorEnum.NOT_GROUP_OWNER);
        AssertUtils.isFalse(
                GroupMemberRoleEnum.MEMBER.name().equals(operator.getRole()),
                BizErrorEnum.NOT_GROUP_OWNER
        );

        // 校验被踢成员存在
        GroupMember target = groupMemberDao.getByGroupAndUser(groupId, targetId);
        AssertUtils.nonNull(target, BizErrorEnum.GROUP_NOT_FOUND);

        target.setLeftAt(new Date());
        target.setUpdatedAt(new Date());
        groupMemberDao.updateById(target);
    }

    /**
     * 查询群成员列表.
     *
     * @param groupId 群聊id
     * @return 群成员响应列表
     */
    @Override
    public List<GroupMemberRespVO> getMemberList(final BigInteger groupId) {
        List<GroupMember> memberList = groupMemberDao.getGroupMemberListByGroupId(groupId);
        if (memberList.isEmpty()) {
            return List.of();
        }
        List<BigInteger> userIdList = memberList.stream()
                .map(GroupMember::getUserId)
                .distinct()
                .toList();
        Map<BigInteger, User> userMap = userDao.listByIds(userIdList).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return memberList.stream().map(member -> {
            GroupMemberRespVO respVO = GroupMemberConvertor.INSTANCE.toRespVO(member);
            User user = userMap.get(member.getUserId());
            if (Objects.nonNull(user)) {
                respVO.setUsername(user.getUsername());
                respVO.setAvatarUrl(user.getAvatarUrl());
            }
            return respVO;
        }).toList();
    }

    /**
     * 查询群成员资产上下文.
     *
     * @param groupId 群聊id
     * @param userId  用户id
     * @return 群成员信息，不是在群成员时返回null
     */
    @Override
    public GroupMemberRespVO getGroupAssetMemberContext(
            final BigInteger groupId,
            final BigInteger userId
    ) {
        Group group = groupDao.getById(groupId);
        if (Objects.isNull(group) || !GroupStatusEnum.ACTIVE.name().equals(group.getStatus())) {
            return null;
        }
        GroupMember groupMember = groupMemberDao.getByGroupAndUser(groupId, userId);
        if (Objects.isNull(groupMember)) {
            return null;
        }
        User user = userDao.getById(userId);
        AssertUtils.nonNull(user, BizErrorEnum.USER_NOT_FOUND);
        GroupMemberRespVO respVO = GroupMemberConvertor.INSTANCE.toRespVO(groupMember);
        respVO.setUsername(user.getUsername());
        return respVO;
    }
}
