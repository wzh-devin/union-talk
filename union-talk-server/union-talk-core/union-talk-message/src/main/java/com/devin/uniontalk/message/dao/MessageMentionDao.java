package com.devin.uniontalk.message.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.infrastructure.message.enums.MessageMentionTypeEnum;
import com.devin.uniontalk.message.domain.command.MessageMentionCommand;
import com.devin.uniontalk.message.domain.entity.Message;
import com.devin.uniontalk.message.domain.entity.MessageMention;
import com.devin.uniontalk.message.mapper.MessageMentionMapper;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 2026/08/06 23:05.
 *
 * <p>
 * 消息提及Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MessageMentionDao extends ServiceImpl<MessageMentionMapper, MessageMention> {

    /**
     * 批量创建消息提及记录.
     *
     * @param message            消息实体
     * @param mentionCommandList 提及输入命令列表
     * @return 提及实体列表
     */
    public List<MessageMention> createMentionList(
            final Message message,
            final List<MessageMentionCommand> mentionCommandList
    ) {
        if (Objects.isNull(mentionCommandList) || mentionCommandList.isEmpty()) {
            return List.of();
        }
        List<MessageMention> mentionList = mentionCommandList.stream()
                .sorted(Comparator.comparing(MessageMentionCommand::getStartOffset))
                .map(command -> {
                    MessageMention mention = new MessageMention();
                    mention.init(message, command);
                    return mention;
                })
                .toList();
        saveBatch(mentionList);
        return mentionList;
    }

    /**
     * 批量查询消息提及记录.
     *
     * @param messageIdList 消息id列表
     * @return 提及实体列表
     */
    public List<MessageMention> getMentionListByMessageIds(final List<BigInteger> messageIdList) {
        if (Objects.isNull(messageIdList) || messageIdList.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(MessageMention::getMessageId, messageIdList)
                .orderByAsc(MessageMention::getMessageId)
                .orderByAsc(MessageMention::getStartOffset)
                .list();
    }

    /**
     * 判断消息是否提及指定Agent.
     *
     * @param messageId 消息id
     * @param agentId   稳定Agent定义id
     * @return 是否存在提及记录
     */
    public boolean existsAgentMention(final BigInteger messageId, final BigInteger agentId) {
        return lambdaQuery()
                .eq(MessageMention::getMessageId, messageId)
                .eq(MessageMention::getMentionType, MessageMentionTypeEnum.AGENT.name())
                .eq(MessageMention::getTargetId, agentId)
                .count() > 0;
    }

    /**
     * 查询消息中的Agent提及记录.
     *
     * @param messageId 消息id
     * @return Agent提及记录，不存在时返回null
     */
    public MessageMention getAgentMention(final BigInteger messageId) {
        return lambdaQuery()
                .eq(MessageMention::getMessageId, messageId)
                .eq(MessageMention::getMentionType, MessageMentionTypeEnum.AGENT.name())
                .orderByAsc(MessageMention::getStartOffset)
                .last("LIMIT 1")
                .one();
    }
}
