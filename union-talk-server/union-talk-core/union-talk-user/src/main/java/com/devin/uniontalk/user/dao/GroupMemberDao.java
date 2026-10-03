package com.devin.uniontalk.user.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.user.domain.entity.GroupMember;
import com.devin.uniontalk.user.mapper.GroupMemberMapper;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 群成员(GroupMember)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMemberDao extends ServiceImpl<GroupMemberMapper, GroupMember> {

    /**
     * 查询指定群聊中的某个成员记录.
     *
     * @param groupId 群聊id
     * @param userId  用户id
     * @return 群成员记录，不存在则返回null
     */
    public GroupMember getByGroupAndUser(final BigInteger groupId, final BigInteger userId) {
        return lambdaQuery()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId)
                .isNull(GroupMember::getLeftAt)
                .one();
    }

    /**
     * 查询群聊的所有在群成员.
     *
     * @param groupId 群聊id
     * @return 群成员列表
     */
    public List<GroupMember> getGroupMemberListByGroupId(final BigInteger groupId) {
        return lambdaQuery()
                .eq(GroupMember::getGroupId, groupId)
                .isNull(GroupMember::getLeftAt)
                .list();
    }

    /**
     * 批量查询群聊在群成员列表.
     *
     * @param idList 群聊id列表
     * @return 群成员列表
     */
    public List<GroupMember> getGroupMemberListByGroupIds(final List<BigInteger> idList) {
        if (Objects.isNull(idList) || idList.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(GroupMember::getGroupId, idList)
                .isNull(GroupMember::getLeftAt)
                .list();
    }

    /**
     * 查询用户加入的所有群聊id列表.
     *
     * @param userId 用户id
     * @return 群聊id列表
     */
    public List<BigInteger> getGroupIdListByUserId(final BigInteger userId) {
        return lambdaQuery()
                .eq(GroupMember::getUserId, userId)
                .isNull(GroupMember::getLeftAt)
                .list()
                .stream()
                .map(GroupMember::getGroupId)
                .toList();
    }

    /**
     * 统计群聊当前在群成员数量.
     *
     * @param groupId 群聊id
     * @return 成员数量
     */
    public long countByGroupId(final BigInteger groupId) {
        return lambdaQuery()
                .eq(GroupMember::getGroupId, groupId)
                .isNull(GroupMember::getLeftAt)
                .count();
    }
}
