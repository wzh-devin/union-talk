package com.devin.uniontalk.message.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.infrastructure.message.enums.ConversationTypeEnum;
import com.devin.uniontalk.message.domain.entity.Conversation;
import com.devin.uniontalk.message.mapper.ConversationMapper;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 2026/05/20 15:20:19.
 *
 * <p>
 *  会话表(Conversation)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationDao extends ServiceImpl<ConversationMapper, Conversation> {

    /**
     * 创建会话.
     *
     * @param type    会话类型
     * @param groupId 群聊id
     * @return 会话实体
     */
    public Conversation createConversation(final ConversationTypeEnum type, final BigInteger groupId) {
        Conversation conversation = new Conversation();
        conversation.setId(IdGenerator.nextIdBigInteger());
        conversation.setType(type.name());
        conversation.setGroupId(groupId);
        conversation.init();
        save(conversation);
        return conversation;
    }

    /**
     * 按群聊id查询群聊会话.
     *
     * @param groupId 群聊id
     * @return 群聊会话
     */
    public Conversation getGroupConversation(final BigInteger groupId) {
        return lambdaQuery()
                .eq(Conversation::getType, ConversationTypeEnum.GROUP.name())
                .eq(Conversation::getGroupId, groupId)
                .one();
    }

    /**
     * 批量查询会话列表.
     *
     * @param conversationIdList 会话id列表
     * @return 会话列表
     */
    public List<Conversation> getConversationListByIds(final List<BigInteger> conversationIdList) {
        if (Objects.isNull(conversationIdList) || conversationIdList.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(Conversation::getId, conversationIdList)
                .list();
    }

    /**
     * 更新会话最后一条消息.
     *
     * @param conversationId 会话id
     * @param lastMsgId     最后一条消息id
     * @param lastMsgAt     最后一条消息时间
     */
    public void updateLastMessage(
            final BigInteger conversationId,
            final BigInteger lastMsgId,
            final Date lastMsgAt
    ) {
        lambdaUpdate()
                .set(Conversation::getLastMsgId, lastMsgId)
                .set(Conversation::getLastMsgAt, lastMsgAt)
                .set(Conversation::getUpdatedAt, new Date())
                .eq(Conversation::getId, conversationId)
                .update();
    }
}
