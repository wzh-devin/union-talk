package com.devin.uniontalk.user.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.user.domain.entity.FriendGroup;
import com.devin.uniontalk.user.mapper.FriendGroupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 好友分组(FriendGroup)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendGroupDao extends ServiceImpl<FriendGroupMapper, FriendGroup> {

    /**
     * 查询用户的所有好友分组.
     *
     * @param userId 用户id
     * @return 好友分组列表
     */
    public List<FriendGroup> listByUserId(final BigInteger userId) {
        return lambdaQuery()
                .eq(FriendGroup::getUserId, userId)
                .orderByAsc(FriendGroup::getSortOrder)
                .list();
    }

    /**
     * 按用户和分组名称查询.
     *
     * @param userId 用户id
     * @param name   分组名称
     * @return 好友分组，不存在则返回null
     */
    public FriendGroup getByUserIdAndName(final BigInteger userId, final String name) {
        return lambdaQuery()
                .eq(FriendGroup::getUserId, userId)
                .eq(FriendGroup::getName, name)
                .one();
    }

    /**
     * 初始化用户默认分组.
     *
     * @param id 用户id
     */
    public void init(final BigInteger id) {
        FriendGroup defaultGroup = new FriendGroup();
        defaultGroup.setId(IdGenerator.nextIdBigInteger());
        defaultGroup.setUserId(id);
        defaultGroup.setName("默认分组");
        defaultGroup.init();
        save(defaultGroup);
    }

    /**
     * 条件更新好友分组信息，仅更新非空字段.
     *
     * @param friendGroup 好友分组实体（id必填，其余字段非空则更新）
     */
    public void updateFriendGroup(final FriendGroup friendGroup) {
        lambdaUpdate()
                .set(StringUtils.hasLength(friendGroup.getName()), FriendGroup::getName, friendGroup.getName())
                .set(Objects.nonNull(friendGroup.getSortOrder()), FriendGroup::getSortOrder, friendGroup.getSortOrder())
                .set(FriendGroup::getUpdatedAt, new Date())
                .eq(FriendGroup::getId, friendGroup.getId())
                .update();
    }
}
