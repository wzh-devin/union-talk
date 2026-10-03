package com.devin.uniontalk.user.service;

import com.devin.uniontalk.user.domain.entity.FriendGroup;
import java.math.BigInteger;
import java.util.List;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 好友分组(FriendGroup)Service层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface FriendGroupService {

    /**
     * 创建好友分组.
     *
     * @param userId    用户id
     * @param name      分组名称
     * @param sortOrder 分组排序
     * @return 创建的好友分组
     */
    FriendGroup createFriendGroup(
            BigInteger userId,
            String name,
            Integer sortOrder
    );

    /**
     * 查询用户的好友分组列表.
     *
     * @param userId 用户id
     * @return 好友分组列表
     */
    List<FriendGroup> getFriendGroupList(BigInteger userId);

    /**
     * 更新好友分组.
     *
     * @param userId    用户id
     * @param groupId   分组id
     * @param name      分组名称
     * @param sortOrder 分组排序
     */
    void updateFriendGroup(
            BigInteger userId,
            BigInteger groupId,
            String name,
            Integer sortOrder
    );

    /**
     * 删除好友分组.
     *
     * @param userId  用户id
     * @param groupId 分组id
     */
    void deleteFriendGroup(BigInteger userId, BigInteger groupId);
}
