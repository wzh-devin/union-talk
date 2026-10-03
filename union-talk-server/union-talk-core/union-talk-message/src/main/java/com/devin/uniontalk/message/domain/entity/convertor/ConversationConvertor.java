package com.devin.uniontalk.message.domain.entity.convertor;

import com.devin.uniontalk.message.domain.entity.Conversation;
import com.devin.uniontalk.message.domain.entity.UserConversation;
import com.devin.uniontalk.message.domain.vo.resp.ConversationRespVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 2026/05/20 16:30.
 *
 * <p>
 * 会话实体转换器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper(
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ConversationConvertor {

    ConversationConvertor INSTANCE = Mappers.getMapper(ConversationConvertor.class);

    /**
     * 将会话实体与用户会话实体转换为响应VO.
     *
     * @param conversation     会话实体
     * @param userConversation 用户会话实体
     * @return 会话响应VO
     */
    @Mappings({
        @Mapping(source = "userConversation.id", target = "userConversationId"),
        @Mapping(source = "conversation.id", target = "conversationId"),
        @Mapping(source = "conversation.type", target = "type"),
        @Mapping(source = "conversation.groupId", target = "groupId"),
        @Mapping(source = "conversation.lastMsgId", target = "lastMsgId"),
        @Mapping(source = "conversation.lastMsgAt", target = "lastMsgAt"),
        @Mapping(source = "userConversation.unreadCount", target = "unreadCount"),
        @Mapping(source = "userConversation.lastReadMsgId", target = "lastReadMsgId"),
        @Mapping(source = "userConversation.mentionUnreadCount", target = "mentionUnreadCount"),
        @Mapping(source = "userConversation.lastMentionMsgId", target = "lastMentionMsgId"),
        @Mapping(source = "userConversation.isPinned", target = "isPinned"),
        @Mapping(source = "userConversation.isMuted", target = "isMuted"),
        @Mapping(source = "userConversation.status", target = "status"),
        @Mapping(source = "userConversation.updatedAt", target = "updatedAt"),
        @Mapping(target = "lastMessage", ignore = true),
        @Mapping(target = "targetUser", ignore = true),
        @Mapping(target = "groupInfo", ignore = true)
    })
    ConversationRespVO toRespVO(Conversation conversation, UserConversation userConversation);
}
