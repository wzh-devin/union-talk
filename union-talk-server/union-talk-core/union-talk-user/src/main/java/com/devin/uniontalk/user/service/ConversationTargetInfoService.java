package com.devin.uniontalk.user.service;

import com.devin.uniontalk.user.domain.model.ConversationTargetInfo;
import java.math.BigInteger;
import java.util.List;

/**
 * 2026/05/31 21:30.
 *
 * <p>
 * 会话目标信息Service层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ConversationTargetInfoService {

    /**
     * 查询会话目标信息.
     *
     * @param userIdList  用户id列表
     * @param groupIdList 群聊id列表
     * @return 会话目标信息
     */
    ConversationTargetInfo getConversationTargetInfo(
            List<BigInteger> userIdList,
            List<BigInteger> groupIdList
    );
}
