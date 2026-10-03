package com.devin.uniontalk.message.service;

import com.devin.uniontalk.base.cursor.model.CursorPageQuery;
import com.devin.uniontalk.base.cursor.model.CursorPageResult;
import com.devin.uniontalk.message.domain.model.ConversationAssetContext;
import com.devin.uniontalk.message.domain.vo.resp.ConversationRespVO;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

/**
 * 2026/05/20 15:20:22.
 *
 * <p>
 *  会话表(Conversation)Service层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ConversationService {

    /**
     * 创建私聊会话.
     *
     * @param fromUserId 发送方用户id
     * @param toUserId   接收方用户id
     * @return 会话id
     */
    BigInteger createPrivateConversation(BigInteger fromUserId, BigInteger toUserId);

    /**
     * 创建群聊会话.
     *
     * @param groupId      群聊id
     * @param memberIdList 群成员id列表
     * @return 会话id
     */
    BigInteger createGroupConversation(BigInteger groupId, List<BigInteger> memberIdList);

    /**
     * 解散群聊关联会话.
     *
     * @param groupId 群聊id
     */
    void dissolveGroupConversation(BigInteger groupId);

    /**
     * 查询当前用户会话分页列表.
     *
     * @param userId      用户id
     * @param cursorQuery 游标分页查询参数
     * @return 会话分页列表
     */
    CursorPageResult<ConversationRespVO, Date> pageByUserId(
            BigInteger userId,
            CursorPageQuery<Date> cursorQuery
    );

    /**
     * 查询当前用户会话详情.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @return 会话详情
     */
    ConversationRespVO getDetail(BigInteger userId, BigInteger conversationId);

    /**
     * 标记会话已读.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @param lastReadMsgId  最后已读消息id
     */
    void read(BigInteger userId, BigInteger conversationId, BigInteger lastReadMsgId);

    /**
     * 判断用户是否属于会话.
     *
     * @param conversationId 会话id
     * @param userId         用户id
     * @return 是否属于会话
     */
    boolean existsConversationMember(BigInteger conversationId, BigInteger userId);

    /**
     * 查询会话资产上下文.
     *
     * @param conversationId 会话id
     * @param operatorUserId 操作用户id
     * @return 会话资产上下文
     */
    ConversationAssetContext getConversationAssetContext(
            BigInteger conversationId,
            BigInteger operatorUserId
    );
}
