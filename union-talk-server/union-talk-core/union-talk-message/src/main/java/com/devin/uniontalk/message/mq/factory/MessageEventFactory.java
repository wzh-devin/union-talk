package com.devin.uniontalk.message.mq.factory;

import com.devin.uniontalk.infrastructure.message.enums.ConversationTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageOutboxEventTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageSenderTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import com.devin.uniontalk.message.domain.entity.Conversation;
import com.devin.uniontalk.message.domain.entity.Message;
import com.devin.uniontalk.message.domain.entity.MessageAgentExtension;
import com.devin.uniontalk.message.domain.entity.MessageMention;
import com.devin.uniontalk.message.domain.vo.resp.ConversationUserInfoRespVO;
import com.devin.uniontalk.message.domain.vo.resp.MessageAssetInfoRespVO;
import com.devin.uniontalk.message.grpc.client.UserConversationTargetInfoGrpcClient;
import com.devin.uniontalk.message.mq.convertor.MessageEventConvertor;
import com.devin.uniontalk.rabbitmq.constant.RabbitMqConstant;
import com.devin.uniontalk.rabbitmq.domain.event.AgentMentionedEvent;
import com.devin.uniontalk.rabbitmq.domain.event.ConversationUpdatedEvent;
import com.devin.uniontalk.rabbitmq.domain.event.MessageCreatedEvent;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 2026/07/28 22:20.
 *
 * <p>
 * 消息事件快照工厂
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEventFactory {

    /**
     * 用户会话目标信息Grpc客户端.
     */
    private final UserConversationTargetInfoGrpcClient userConversationTargetInfoGrpcClient;

    /**
     * 构建消息创建事件.
     *
     * @param message            消息记录
     * @param mentionList        结构化提及列表
     * @param assetInfo          消息资产文件信息
     * @param extension          AI消息扩展
     * @param receiverUserIdList 接收用户id列表
     * @return 消息创建事件
     */
    public MessageCreatedEvent createMessageCreatedEvent(
            final Message message,
            final List<MessageMention> mentionList,
            final MessageAssetInfoRespVO assetInfo,
            final MessageAgentExtension extension,
            final List<BigInteger> receiverUserIdList
    ) {
        return MessageCreatedEvent.builder()
                .eventId(messageCreatedEventId(message.getId()))
                .eventType(MessageOutboxEventTypeEnum.MESSAGE_CREATED)
                .schemaVersion(RabbitMqConstant.MESSAGE_CREATED_EVENT_SCHEMA_VERSION)
                .messageId(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderType(MessageSenderTypeEnum.valueOf(message.getSenderType()))
                .senderUser(fetchSenderUserSnapshot(message.getSenderId()))
                .senderAgent(MessageEventConvertor.INSTANCE.toSenderAgentSnapshot(extension))
                .type(MessageTypeEnum.valueOf(message.getType()))
                .content(message.getContent())
                .assetInfo(MessageEventConvertor.INSTANCE.toAssetInfoSnapshot(assetInfo))
                .quoteMsgId(message.getQuoteMsgId())
                .mentionList(Objects.isNull(mentionList)
                        ? List.of()
                        : MessageEventConvertor.INSTANCE.toMentionSnapshotList(mentionList))
                .agentRunId(Objects.isNull(extension) ? null : extension.getAgentRunId())
                .triggerMessageId(Objects.isNull(extension) ? null : extension.getTriggerMessageId())
                .citationList(Objects.isNull(extension)
                        ? List.of()
                        : MessageEventConvertor.INSTANCE.toCitationSnapshotList(extension.getCitationList()))
                .recalled(message.getRecalled())
                .receiverUserIdList(receiverUserIdList)
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * 构建会话更新事件.
     *
     * @param conversation       会话记录
     * @param message            最新消息记录
     * @param receiverUserIdList 接收用户id列表
     * @return 会话更新事件
     */
    public ConversationUpdatedEvent createConversationUpdatedEvent(
            final Conversation conversation,
            final Message message,
            final List<BigInteger> receiverUserIdList
    ) {
        return ConversationUpdatedEvent.builder()
                .eventId(conversationUpdatedEventId(message.getId()))
                .conversationId(conversation.getId())
                .type(ConversationTypeEnum.valueOf(conversation.getType()))
                .groupId(conversation.getGroupId())
                .lastMsgId(message.getId())
                .lastMsgAt(message.getCreatedAt())
                .senderId(message.getSenderId())
                .senderType(MessageSenderTypeEnum.valueOf(message.getSenderType()))
                .receiverUserIdList(receiverUserIdList)
                .updatedAt(new Date())
                .build();
    }

    /**
     * 构建Agent提及事件.
     *
     * @param messageCreatedEvent 消息创建事件
     * @param agentId             被提及的稳定Agent定义id
     * @return Agent提及事件
     */
    public AgentMentionedEvent createAgentMentionedEvent(
            final MessageCreatedEvent messageCreatedEvent,
            final BigInteger agentId
    ) {
        MessageCreatedEvent.SenderUserSnapshot senderUser = messageCreatedEvent.getSenderUser();
        return AgentMentionedEvent.builder()
                .eventId(agentMentionedEventId(messageCreatedEvent.getMessageId()))
                .eventType(MessageOutboxEventTypeEnum.AGENT_MENTIONED)
                .schemaVersion(RabbitMqConstant.AGENT_MENTIONED_EVENT_SCHEMA_VERSION)
                .triggerMessageId(messageCreatedEvent.getMessageId())
                .conversationId(messageCreatedEvent.getConversationId())
                .agentId(agentId)
                .requesterUserId(messageCreatedEvent.getSenderId())
                .requesterDisplayName(Objects.isNull(senderUser) ? null : senderUser.getUsername())
                .messageType(messageCreatedEvent.getType())
                .occurredAt(messageCreatedEvent.getCreatedAt())
                .build();
    }

    /**
     * 构建消息创建事件id.
     *
     * @param messageId 消息id
     * @return 事件id
     */
    public String messageCreatedEventId(final BigInteger messageId) {
        return "message-created:" + messageId;
    }

    /**
     * 构建会话更新事件id.
     *
     * @param messageId 消息id
     * @return 事件id
     */
    public String conversationUpdatedEventId(final BigInteger messageId) {
        return "conversation-updated:" + messageId;
    }

    /**
     * 构建Agent提及事件id.
     *
     * @param messageId 消息id
     * @return 事件id
     */
    public String agentMentionedEventId(final BigInteger messageId) {
        return "agent-mentioned:" + messageId;
    }

    /**
     * 获取发送者用户快照.
     *
     * @param senderId 发送者id
     * @return 发送者用户快照
     */
    private MessageCreatedEvent.SenderUserSnapshot fetchSenderUserSnapshot(final BigInteger senderId) {
        if (Objects.isNull(senderId)) {
            return null;
        }
        try {
            UserConversationTargetInfoGrpcClient.TargetInfo targetInfo =
                    userConversationTargetInfoGrpcClient.getConversationTargetInfo(List.of(senderId), List.of());
            Map<BigInteger, ConversationUserInfoRespVO> userInfoMap = targetInfo.getUserInfoMap();
            if (Objects.isNull(userInfoMap)) {
                return null;
            }
            ConversationUserInfoRespVO userInfo = userInfoMap.get(senderId);
            if (Objects.isNull(userInfo)) {
                return null;
            }
            return MessageEventConvertor.INSTANCE.toSenderUserSnapshot(userInfo);
        } catch (Exception e) {
            log.warn("查询消息发送者用户快照失败, senderId={}", senderId, e);
            return null;
        }
    }
}
