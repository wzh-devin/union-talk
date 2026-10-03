package com.devin.uniontalk.user.service;

import com.devin.uniontalk.user.domain.vo.resp.FriendRequestRespVO;
import java.math.BigInteger;
import java.util.List;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 好友申请(FriendRequest)Service层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface FriendRequestService {

    /**
     * 发送好友申请.
     *
     * @param fromUserId 申请人id
     * @param toUserCode 目标用户code
     * @param applyMsg   申请信息
     */
    void sendRequest(
            BigInteger fromUserId,
            String toUserCode,
            String applyMsg
    );

    /**
     * 处理好友申请.
     *
     * @param userId        当前用户id
     * @param requestId     申请id
     * @param accept        是否接受
     * @param friendGroupId 好友分组id（接受时使用）
     * @param remark        好友备注（接受时使用）
     */
    void handleRequest(
            BigInteger userId,
            BigInteger requestId,
            Boolean accept,
            BigInteger friendGroupId,
            String remark
    );

    /**
     * 查询收到的好友申请列表.
     *
     * @param userId 用户id
     * @return 好友申请响应列表
     */
    List<FriendRequestRespVO> getReceivedRequestList(BigInteger userId);

    /**
     * 查询发出的好友申请列表.
     *
     * @param userId 用户id
     * @return 好友申请响应列表
     */
    List<FriendRequestRespVO> getSentRequestList(BigInteger userId);
}
