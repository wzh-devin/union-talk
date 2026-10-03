package com.devin.uniontalk.user.service;

import com.devin.uniontalk.user.domain.vo.resp.GroupRespVO;
import java.math.BigInteger;
import java.util.List;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 群聊(Group)Service层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface GroupService {

    /**
     * 创建群聊.
     *
     * @param ownerId     群主id
     * @param name        群聊名称
     * @param avatarUrl   群聊头像
     * @param description 群聊描述
     * @param memberLimit 人员限制
     * @param userIdList  群聊人员id列表
     * @return 群聊响应VO
     */
    GroupRespVO createGroup(
            BigInteger ownerId,
            String name,
            String avatarUrl,
            String description,
            Integer memberLimit,
            List<BigInteger> userIdList
    );

    /**
     * 更新群聊信息.
     *
     * @param userId      操作用户id
     * @param groupId     群聊id
     * @param name        群聊名称
     * @param avatarUrl   群聊头像
     * @param description 群聊描述
     * @param memberLimit 人员限制
     */
    void updateGroup(
            BigInteger userId,
            BigInteger groupId,
            String name,
            String avatarUrl,
            String description,
            Integer memberLimit
    );

    /**
     * 解散群聊.
     *
     * @param userId  操作用户id
     * @param groupId 群聊id
     */
    void dissolveGroup(BigInteger userId, BigInteger groupId);

    /**
     * 获取群聊详情.
     *
     * @param groupId 群聊id
     * @return 群聊响应VO
     */
    GroupRespVO getGroupDetail(BigInteger groupId);
}
