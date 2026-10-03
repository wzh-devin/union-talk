package com.devin.uniontalk.message.service;

import com.devin.uniontalk.base.cursor.model.CursorPageQuery;
import com.devin.uniontalk.base.cursor.model.CursorPageResult;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import com.devin.uniontalk.message.domain.command.MessageMentionCommand;
import com.devin.uniontalk.message.domain.vo.resp.MessageRespVO;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

/**
 * 2026/05/20 15:20:24.
 *
 * <p>
 *  消息表(Message)Service层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface MessageService {

    /**
     * 发送消息.
     *
     * @param senderId       发送者id
     * @param conversationId 会话id
     * @param type           消息类型
     * @param content        消息内容
     * @param quoteMsgId     引用消息id
     * @param mentionCommandList 结构化提及列表
     * @return 消息响应
     */
    MessageRespVO send(
            BigInteger senderId,
            BigInteger conversationId,
            MessageTypeEnum type,
            String content,
            BigInteger quoteMsgId,
            List<MessageMentionCommand> mentionCommandList
    );

    /**
     * 查询会话消息分页列表.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @param cursorQuery    游标分页查询参数
     * @return 消息分页列表
     */
    CursorPageResult<MessageRespVO, Date> pageByConversationId(
            BigInteger userId,
            BigInteger conversationId,
            CursorPageQuery<Date> cursorQuery
    );
}
