package com.devin.uniontalk.user.service;

import com.devin.uniontalk.user.domain.vo.resp.FriendRespVO;
import java.math.BigInteger;
import java.util.List;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 用户关系(UserRelation)Service层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface UserRelationService {

    /**
     * 查询用户的好友列表.
     *
     * @param userId 用户id
     * @return 好友响应列表
     */
    List<FriendRespVO> getFriendList(BigInteger userId);

    /**
     * 修改好友备注.
     *
     * @param userId   用户id
     * @param targetId 对方id
     * @param remark   备注
     */
    void updateRemark(
            BigInteger userId,
            BigInteger targetId,
            String remark
    );

    /**
     * 移动好友到指定分组.
     *
     * @param userId        用户id
     * @param targetId      对方id
     * @param friendGroupId 目标分组id
     */
    void moveFriendGroup(
            BigInteger userId,
            BigInteger targetId,
            BigInteger friendGroupId
    );

    /**
     * 删除好友.
     *
     * @param userId   用户id
     * @param targetId 对方id
     */
    void deleteFriend(BigInteger userId, BigInteger targetId);

    /**
     * 拉黑好友.
     *
     * @param userId   用户id
     * @param targetId 对方id
     */
    void blockFriend(BigInteger userId, BigInteger targetId);

    /**
     * 取消拉黑.
     *
     * @param userId   用户id
     * @param targetId 对方id
     */
    void unblockFriend(BigInteger userId, BigInteger targetId);
}
