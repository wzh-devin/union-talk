package com.devin.uniontalk.user.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.user.domain.entity.Group;
import com.devin.uniontalk.user.mapper.GroupMapper;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 群聊(Group)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupDao extends ServiceImpl<GroupMapper, Group> {

    /**
     * 查询用户创建的群聊列表.
     *
     * @param ownerId 群主id
     * @return 群聊列表
     */
    public List<Group> getGroupListByOwnerId(final BigInteger ownerId) {
        return lambdaQuery()
                .eq(Group::getOwnerId, ownerId)
                .list();
    }

    /**
     * 批量查询群聊列表.
     *
     * @param idList 群聊id列表
     * @return 群聊列表
     */
    public List<Group> getGroupListByIds(final List<BigInteger> idList) {
        if (Objects.isNull(idList) || idList.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(Group::getId, idList)
                .list();
    }

    /**
     * 条件更新群聊信息，仅更新非空字段.
     *
     * @param group 群聊实体（id必填，其余字段非空则更新）
     */
    public void updateGroup(final Group group) {
        lambdaUpdate()
                .set(StringUtils.hasLength(group.getName()), Group::getName, group.getName())
                .set(StringUtils.hasLength(group.getAvatarUrl()), Group::getAvatarUrl, group.getAvatarUrl())
                .set(StringUtils.hasLength(group.getDescription()), Group::getDescription, group.getDescription())
                .set(Objects.nonNull(group.getMemberLimit()), Group::getMemberLimit, group.getMemberLimit())
                .set(Group::getUpdatedAt, new Date())
                .eq(Group::getId, group.getId())
                .update();
    }
}
