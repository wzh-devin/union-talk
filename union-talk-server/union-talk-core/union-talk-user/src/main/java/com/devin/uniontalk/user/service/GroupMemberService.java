package com.devin.uniontalk.user.service;

import com.devin.uniontalk.user.domain.vo.resp.GroupMemberRespVO;
import java.math.BigInteger;
import java.util.List;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 群成员(GroupMember)Service层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface GroupMemberService {

    /**
     * 邀请用户入群.
     *
     * @param userId     操作用户id
     * @param groupId    群聊id
     * @param userIdList 被邀请用户id列表
     */
    void inviteMembers(
            BigInteger userId,
            BigInteger groupId,
            List<BigInteger> userIdList
    );

    /**
     * 退出群聊.
     *
     * @param userId  用户id
     * @param groupId 群聊id
     */
    void leaveGroup(BigInteger userId, BigInteger groupId);

    /**
     * 踢出群成员.
     *
     * @param operatorId 操作人id
     * @param groupId    群聊id
     * @param targetId   被踢用户id
     */
    void kickMember(
            BigInteger operatorId,
            BigInteger groupId,
            BigInteger targetId
    );

    /**
     * 查询群成员列表.
     *
     * @param groupId 群聊id
     * @return 群成员响应列表
     */
    List<GroupMemberRespVO> getMemberList(BigInteger groupId);

    /**
     * 查询群成员资产上下文.
     *
     * @param groupId 群聊id
     * @param userId  用户id
     * @return 群成员信息，不是在群成员时返回null
     */
    GroupMemberRespVO getGroupAssetMemberContext(BigInteger groupId, BigInteger userId);
}
