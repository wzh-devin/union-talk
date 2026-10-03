package com.devin.uniontalk.message.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.base.cursor.model.CursorPageQuery;
import com.devin.uniontalk.base.cursor.model.CursorPageResult;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.datasource.utils.PgCursorPageUtils;
import com.devin.uniontalk.infrastructure.message.enums.UserConversationStatusEnum;
import com.devin.uniontalk.message.domain.entity.UserConversation;
import com.devin.uniontalk.message.mapper.UserConversationMapper;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 2026/05/20 15:20:24.
 *
 * <p>
 *  用户会话(UserConversation)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserConversationDao extends ServiceImpl<UserConversationMapper, UserConversation> {

    /**
     * 初始化用户会话列表.
     *
     * @param conversationId 会话id
     * @param memberIdList   成员id列表
     */
    public void initUserConversations(final BigInteger conversationId, final List<BigInteger> memberIdList) {
        if (Objects.isNull(memberIdList) || memberIdList.isEmpty()) {
            return;
        }
        saveBatch(memberIdList.stream()
                .map(memberId -> buildUserConversation(conversationId, memberId))
                .toList());
    }

    /**
     * 查询用户会话分页列表.
     *
     * @param userId      用户id
     * @param cursorQuery 游标分页查询参数
     * @return 用户会话分页列表
     */
    public CursorPageResult<UserConversation, Date> pageByUserId(
            final BigInteger userId,
            final CursorPageQuery<Date> cursorQuery
    ) {
        LambdaQueryWrapper<UserConversation> queryWrapper = new LambdaQueryWrapper<UserConversation>()
                .eq(UserConversation::getUserId, userId)
                .in(
                        UserConversation::getStatus,
                        UserConversationStatusEnum.ACTIVE.name()
                );
        return PgCursorPageUtils.page(
                this,
                queryWrapper,
                cursorQuery,
                UserConversation::getUpdatedAt,
                UserConversation::getId,
                UserConversation::getUpdatedAt,
                UserConversation::getId
        );
    }

    /**
     * 查询用户会话.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @return 用户会话
     */
    public UserConversation getByUserIdAndConversationId(
            final BigInteger userId,
            final BigInteger conversationId
    ) {
        return lambdaQuery()
                .eq(UserConversation::getUserId, userId)
                .eq(UserConversation::getConversationId, conversationId)
                .one();
    }

    /**
     * 批量查询会话下的用户会话.
     *
     * @param conversationId 会话id
     * @return 用户会话列表
     */
    public List<UserConversation> getUserConversationListByConversationId(final BigInteger conversationId) {
        return lambdaQuery()
                .eq(UserConversation::getConversationId, conversationId)
                .list();
    }

    /**
     * 查询会话内未解散的用户id列表.
     *
     * @param conversationId 会话id
     * @return 用户id列表
     */
    public List<BigInteger> getAvailableUserIdListByConversationId(final BigInteger conversationId) {
        return lambdaQuery()
                .select(UserConversation::getUserId)
                .eq(UserConversation::getConversationId, conversationId)
                .ne(UserConversation::getStatus, UserConversationStatusEnum.DISSOLVED.name())
                .list()
                .stream()
                .map(UserConversation::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 判断用户是否仍在有效会话中.
     *
     * @param conversationId 会话id
     * @param userId         用户id
     * @return 仍在有效会话中返回true
     */
    public boolean existsAvailableByConversationIdAndUserId(
            final BigInteger conversationId,
            final BigInteger userId
    ) {
        return lambdaQuery()
                .eq(UserConversation::getConversationId, conversationId)
                .eq(UserConversation::getUserId, userId)
                .ne(UserConversation::getStatus, UserConversationStatusEnum.DISSOLVED.name())
                .exists();
    }

    /**
     * 刷新指定成员的会话展示时间.
     *
     * @param conversationId 会话id
     * @param userIdList     用户id列表
     */
    public void refreshMemberConversation(
            final BigInteger conversationId,
            final List<BigInteger> userIdList
    ) {
        if (Objects.isNull(userIdList) || userIdList.isEmpty()) {
            return;
        }
        lambdaUpdate()
                .set(UserConversation::getUpdatedAt, new Date())
                .set(UserConversation::getStatus, UserConversationStatusEnum.ACTIVE.name())
                .set(UserConversation::getHiddenAt, null)
                .eq(UserConversation::getConversationId, conversationId)
                .in(UserConversation::getUserId, userIdList)
                .update();
    }

    /**
     * 标记会话为已解散.
     *
     * @param conversationId 会话id
     */
    public void dissolveByConversationId(final BigInteger conversationId) {
        lambdaUpdate()
                .set(UserConversation::getStatus, UserConversationStatusEnum.DISSOLVED.name())
                .set(UserConversation::getUpdatedAt, new Date())
                .eq(UserConversation::getConversationId, conversationId)
                .update();
    }

    /**
     * 增加接收方未读数.
     *
     * @param conversationId  会话id
     * @param senderId        发送者id
     * @param receiverUserIdList 接收用户id列表
     */
    public void increaseUnreadForReceivers(
            final BigInteger conversationId,
            final BigInteger senderId,
            final List<BigInteger> receiverUserIdList
    ) {
        if (Objects.isNull(receiverUserIdList) || receiverUserIdList.isEmpty()) {
            return;
        }
        lambdaUpdate()
                .setSql("unread_count = unread_count + 1")
                .set(UserConversation::getUpdatedAt, new Date())
                .eq(UserConversation::getConversationId, conversationId)
                .in(UserConversation::getUserId, receiverUserIdList)
                .ne(UserConversation::getUserId, senderId)
                .update();
    }

    /**
     * 增加Agent消息的成员未读数.
     *
     * @param conversationId 会话id
     * @param userIdList     接收用户id列表
     */
    public void increaseUnreadForAgentMessage(
            final BigInteger conversationId,
            final List<BigInteger> userIdList
    ) {
        if (Objects.isNull(userIdList) || userIdList.isEmpty()) {
            return;
        }
        lambdaUpdate()
                .setSql("unread_count = unread_count + 1")
                .set(UserConversation::getUpdatedAt, new Date())
                .eq(UserConversation::getConversationId, conversationId)
                .in(UserConversation::getUserId, userIdList)
                .update();
    }

    /**
     * 增加用户提及未读数.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @param messageId      提及消息id
     */
    public void increaseMentionUnread(
            final BigInteger userId,
            final BigInteger conversationId,
            final BigInteger messageId
    ) {
        lambdaUpdate()
                .setSql("mention_unread_count = mention_unread_count + 1")
                .set(UserConversation::getLastMentionMsgId, messageId)
                .set(UserConversation::getUpdatedAt, new Date())
                .eq(UserConversation::getUserId, userId)
                .eq(UserConversation::getConversationId, conversationId)
                .update();
    }

    /**
     * 清空用户会话未读数.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @param lastReadMsgId  最后已读消息id
     */
    public void clearUnread(
            final BigInteger userId,
            final BigInteger conversationId,
            final BigInteger lastReadMsgId
    ) {
        lambdaUpdate()
                .set(UserConversation::getUnreadCount, 0)
                .set(UserConversation::getMentionUnreadCount, 0)
                .set(Objects.nonNull(lastReadMsgId), UserConversation::getLastReadMsgId, lastReadMsgId)
                .set(UserConversation::getUpdatedAt, new Date())
                .eq(UserConversation::getUserId, userId)
                .eq(UserConversation::getConversationId, conversationId)
                .update();
    }

    /**
     * 构建用户会话实体.
     *
     * @param conversationId 会话id
     * @param userId         用户id
     * @return 用户会话实体
     */
    private UserConversation buildUserConversation(final BigInteger conversationId, final BigInteger userId) {
        UserConversation userConversation = new UserConversation();
        userConversation.setId(IdGenerator.nextIdBigInteger());
        userConversation.setConversationId(conversationId);
        userConversation.setUserId(userId);
        userConversation.setUnreadCount(0);
        userConversation.setIsPinned(Boolean.FALSE);
        userConversation.setIsMuted(Boolean.FALSE);
        userConversation.setStatus(UserConversationStatusEnum.ACTIVE.name());
        userConversation.init();
        return userConversation;
    }
}
