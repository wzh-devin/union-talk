package com.devin.uniontalk.user.service.impl;

import com.devin.uniontalk.infrastructure.user.enums.GroupStatusEnum;
import com.devin.uniontalk.infrastructure.user.enums.UserStatusEnum;
import com.devin.uniontalk.user.dao.GroupDao;
import com.devin.uniontalk.user.dao.GroupMemberDao;
import com.devin.uniontalk.user.dao.UserDao;
import com.devin.uniontalk.user.domain.entity.Group;
import com.devin.uniontalk.user.domain.entity.GroupMember;
import com.devin.uniontalk.user.domain.entity.User;
import com.devin.uniontalk.user.domain.model.ConversationTargetInfo;
import com.devin.uniontalk.user.service.ConversationTargetInfoService;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 2026/05/31 21:30.
 *
 * <p>
 * 会话目标信息ServiceImpl层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationTargetInfoServiceImpl implements ConversationTargetInfoService {

    /**
     * 用户 Dao.
     */
    private final UserDao userDao;

    /**
     * 群聊 Dao.
     */
    private final GroupDao groupDao;

    /**
     * 群成员 Dao.
     */
    private final GroupMemberDao groupMemberDao;

    /**
     * 查询会话目标信息.
     *
     * @param userIdList  用户id列表
     * @param groupIdList 群聊id列表
     * @return 会话目标信息
     */
    @Override
    public ConversationTargetInfo getConversationTargetInfo(
            final List<BigInteger> userIdList,
            final List<BigInteger> groupIdList
    ) {
        return ConversationTargetInfo.builder()
                .userInfoList(getUserInfoList(userIdList))
                .groupInfoList(getGroupInfoList(groupIdList))
                .build();
    }

    /**
     * 查询用户信息列表.
     *
     * @param userIdList 用户id列表
     * @return 用户信息列表
     */
    private List<ConversationTargetInfo.UserInfo> getUserInfoList(final List<BigInteger> userIdList) {
        List<BigInteger> idList = getDistinctIdList(userIdList);
        if (idList.isEmpty()) {
            return List.of();
        }
        return userDao.getUserListByIds(idList)
                .stream()
                .map(this::toUserInfo)
                .toList();
    }

    /**
     * 查询群聊信息列表.
     *
     * @param groupIdList 群聊id列表
     * @return 群聊信息列表
     */
    private List<ConversationTargetInfo.GroupInfo> getGroupInfoList(final List<BigInteger> groupIdList) {
        List<BigInteger> idList = getDistinctIdList(groupIdList);
        if (idList.isEmpty()) {
            return List.of();
        }
        Map<BigInteger, Long> memberCountMap = getGroupMemberCountMap(idList);
        return groupDao.getGroupListByIds(idList)
                .stream()
                .map(group -> toGroupInfo(group, memberCountMap))
                .toList();
    }

    /**
     * 查询群成员数量映射.
     *
     * @param groupIdList 群聊id列表
     * @return 群成员数量映射
     */
    private Map<BigInteger, Long> getGroupMemberCountMap(final List<BigInteger> groupIdList) {
        return groupMemberDao.getGroupMemberListByGroupIds(groupIdList)
                .stream()
                .collect(Collectors.groupingBy(GroupMember::getGroupId, Collectors.counting()));
    }

    /**
     * 去重id列表.
     *
     * @param idList id列表
     * @return 去重后的id列表
     */
    private List<BigInteger> getDistinctIdList(final List<BigInteger> idList) {
        if (Objects.isNull(idList) || idList.isEmpty()) {
            return List.of();
        }
        return idList.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 转换用户信息.
     *
     * @param user 用户实体
     * @return 用户信息
     */
    private ConversationTargetInfo.UserInfo toUserInfo(final User user) {
        return ConversationTargetInfo.UserInfo.builder()
                .userId(user.getId())
                .code(user.getCode())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .status(UserStatusEnum.valueOf(user.getStatus()))
                .build();
    }

    /**
     * 转换群聊信息.
     *
     * @param group          群聊实体
     * @param memberCountMap 群成员数量映射
     * @return 群聊信息
     */
    private ConversationTargetInfo.GroupInfo toGroupInfo(
            final Group group,
            final Map<BigInteger, Long> memberCountMap
    ) {
        return ConversationTargetInfo.GroupInfo.builder()
                .groupId(group.getId())
                .name(group.getName())
                .avatarUrl(group.getAvatarUrl())
                .description(group.getDescription())
                .ownerId(group.getOwnerId())
                .memberCount(memberCountMap.getOrDefault(group.getId(), 0L))
                .status(GroupStatusEnum.valueOf(group.getStatus()))
                .build();
    }
}
