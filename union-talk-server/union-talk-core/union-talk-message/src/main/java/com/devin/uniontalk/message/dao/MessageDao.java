package com.devin.uniontalk.message.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.base.cursor.model.CursorPageQuery;
import com.devin.uniontalk.base.cursor.model.CursorPageResult;
import com.devin.uniontalk.datasource.utils.PgCursorPageUtils;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import com.devin.uniontalk.message.domain.entity.Message;
import com.devin.uniontalk.message.mapper.MessageMapper;
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
 *  消息表(Message)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDao extends ServiceImpl<MessageMapper, Message> {

    /**
     * 创建用户消息.
     *
     * @param senderId       发送者id
     * @param conversationId 会话id
     * @param type           消息类型
     * @param content        消息内容
     * @param quoteMsgId     引用消息id
     * @return 消息实体
     */
    public Message createUserMessage(
            final BigInteger senderId,
            final BigInteger conversationId,
            final MessageTypeEnum type,
            final String content,
            final BigInteger quoteMsgId
    ) {
        Message message = new Message();
        message.initUserMessage(
                senderId,
                conversationId,
                type,
                content,
                quoteMsgId
        );
        save(message);
        return message;
    }

    /**
     * 创建Agent回复消息.
     *
     * @param conversationId  会话id
     * @param content         回复内容
     * @param triggerMessageId 触发消息id
     * @return 消息实体
     */
    public Message createAgentMessage(
            final BigInteger conversationId,
            final String content,
            final BigInteger triggerMessageId
    ) {
        Message message = new Message();
        message.initAgentMessage(conversationId, content, triggerMessageId);
        save(message);
        return message;
    }

    /**
     * 查询并锁定消息.
     *
     * @param messageId 消息id
     * @return 消息实体
     */
    public Message getByIdForUpdate(final BigInteger messageId) {
        return lambdaQuery()
                .eq(Message::getId, messageId)
                .last("FOR UPDATE")
                .one();
    }

    /**
     * 批量查询消息列表.
     *
     * @param messageIdList 消息id列表
     * @return 消息列表
     */
    public List<Message> getMessageListByIds(final List<BigInteger> messageIdList) {
        if (Objects.isNull(messageIdList) || messageIdList.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(Message::getId, messageIdList)
                .list();
    }

    /**
     * 查询会话消息分页列表.
     *
     * @param conversationId 会话id
     * @param cursorQuery    游标分页查询参数
     * @return 消息分页列表
     */
    public CursorPageResult<Message, Date> pageByConversationId(
            final BigInteger conversationId,
            final CursorPageQuery<Date> cursorQuery
    ) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getRecalled, Boolean.FALSE)
                .isNull(Message::getDeletedAt);
        return PgCursorPageUtils.page(
                this,
                queryWrapper,
                cursorQuery,
                Message::getCreatedAt,
                Message::getId,
                Message::getCreatedAt,
                Message::getId
        );
    }

    /**
     * 查询触发消息之前的最近文本消息.
     *
     * @param conversationId   会话id
     * @param triggerMessage   触发消息
     * @param recentMessageLimit 最近消息数量上限
     * @return 按时间倒序排列的最近文本消息
     */
    public List<Message> getRecentTextMessageListBefore(
            final BigInteger conversationId,
            final Message triggerMessage,
            final Integer recentMessageLimit
    ) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getType, MessageTypeEnum.TEXT.name())
                .eq(Message::getRecalled, Boolean.FALSE)
                .isNull(Message::getDeletedAt)
                .and(wrapper -> wrapper
                        .lt(Message::getCreatedAt, triggerMessage.getCreatedAt())
                        .or(orWrapper -> orWrapper
                                .eq(Message::getCreatedAt, triggerMessage.getCreatedAt())
                                .lt(Message::getId, triggerMessage.getId())))
                .orderByDesc(Message::getCreatedAt)
                .orderByDesc(Message::getId);
        return page(Page.of(1, recentMessageLimit, false), queryWrapper).getRecords();
    }
}
