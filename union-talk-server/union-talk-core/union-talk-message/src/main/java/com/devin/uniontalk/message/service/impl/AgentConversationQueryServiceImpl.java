package com.devin.uniontalk.message.service.impl;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.infrastructure.message.constant.MessageAgentConstant;
import com.devin.uniontalk.infrastructure.message.enums.AgentConversationRoleEnum;
import com.devin.uniontalk.infrastructure.message.enums.ConversationTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageMentionTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageSenderTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import com.devin.uniontalk.message.dao.ConversationDao;
import com.devin.uniontalk.message.dao.ConversationMemberDao;
import com.devin.uniontalk.message.dao.MessageDao;
import com.devin.uniontalk.message.dao.MessageMentionDao;
import com.devin.uniontalk.message.dao.UserConversationDao;
import com.devin.uniontalk.message.domain.entity.Conversation;
import com.devin.uniontalk.message.domain.entity.ConversationMember;
import com.devin.uniontalk.message.domain.entity.Message;
import com.devin.uniontalk.message.domain.entity.MessageMention;
import com.devin.uniontalk.message.domain.model.AgentConversationContext;
import com.devin.uniontalk.message.domain.model.AgentConversationMessage;
import com.devin.uniontalk.message.domain.model.AgentConversationPermission;
import com.devin.uniontalk.message.domain.vo.resp.ConversationUserInfoRespVO;
import com.devin.uniontalk.message.grpc.client.UserConversationTargetInfoGrpcClient;
import com.devin.uniontalk.message.grpc.client.UserGroupMemberPermissionGrpcClient;
import com.devin.uniontalk.message.service.AgentConversationQueryService;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 2026/07/31 13:30.
 *
 * <p>
 * Agent会话权限和紧凑上下文查询实现
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AgentConversationQueryServiceImpl implements AgentConversationQueryService {

    /**
     * 会话Dao.
     */
    private final ConversationDao conversationDao;

    /**
     * 会话成员Dao.
     */
    private final ConversationMemberDao conversationMemberDao;

    /**
     * 消息Dao.
     */
    private final MessageDao messageDao;

    /**
     * 消息提及Dao.
     */
    private final MessageMentionDao messageMentionDao;

    /**
     * 用户会话Dao.
     */
    private final UserConversationDao userConversationDao;

    /**
     * 用户会话目标信息Grpc客户端.
     */
    private final UserConversationTargetInfoGrpcClient userConversationTargetInfoGrpcClient;

    /**
     * 用户群成员权限Grpc客户端.
     */
    private final UserGroupMemberPermissionGrpcClient userGroupMemberPermissionGrpcClient;

    /**
     * 查询Agent回答使用的紧凑会话上下文.
     *
     * @param conversationId     会话id
     * @param triggerMessageId   触发消息id
     * @param recentMessageLimit 最近消息数量上限
     * @return Agent紧凑会话上下文
     */
    @Override
    public AgentConversationContext getContext(
            final BigInteger conversationId,
            final BigInteger triggerMessageId,
            final Integer recentMessageLimit
    ) {
        AssertUtils.nonNull(conversationId, BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        AssertUtils.nonNull(triggerMessageId, BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        AssertUtils.isTrue(
                Objects.nonNull(recentMessageLimit)
                        && recentMessageLimit >= MessageAgentConstant.CONTEXT_RECENT_MESSAGE_MIN_COUNT
                        && recentMessageLimit <= MessageAgentConstant.CONTEXT_RECENT_MESSAGE_MAX_COUNT,
                BizErrorEnum.AGENT_REPLY_PARAM_INVALID
        );
        Message triggerMessage = messageDao.getById(triggerMessageId);
        AssertUtils.nonNull(triggerMessage, BizErrorEnum.AGENT_REPLY_TRIGGER_INVALID);
        MessageMention agentMention = messageMentionDao.getAgentMention(triggerMessageId);
        AssertUtils.isTrue(
                Objects.equals(conversationId, triggerMessage.getConversationId())
                        && MessageSenderTypeEnum.USER.name().equals(triggerMessage.getSenderType())
                        && MessageTypeEnum.TEXT.name().equals(triggerMessage.getType())
                        && Objects.nonNull(agentMention)
                        && MessageMentionTypeEnum.AGENT.name().equals(agentMention.getMentionType())
                        && !Boolean.TRUE.equals(triggerMessage.getRecalled())
                        && Objects.isNull(triggerMessage.getDeletedAt()),
                BizErrorEnum.AGENT_REPLY_TRIGGER_INVALID
        );

        List<Message> recentMessageList = new ArrayList<>(
                messageDao.getRecentTextMessageListBefore(
                        conversationId,
                        triggerMessage,
                        recentMessageLimit
                )
        );
        Collections.reverse(recentMessageList);
        List<BigInteger> senderIdList = recentMessageList.stream()
                .filter(message -> MessageSenderTypeEnum.USER.name().equals(message.getSenderType()))
                .map(Message::getSenderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<BigInteger, ConversationUserInfoRespVO> userInfoMap =
                userConversationTargetInfoGrpcClient
                        .getConversationTargetInfo(senderIdList, List.of())
                        .getUserInfoMap();
        List<AgentConversationMessage> contextMessageList = recentMessageList.stream()
                .map(message -> AgentConversationMessage.builder()
                        .messageId(message.getId())
                        .senderType(MessageSenderTypeEnum.valueOf(message.getSenderType()))
                        .senderDisplayName(getSenderDisplayName(message, userInfoMap))
                        .content(message.getContent())
                        .createdAt(message.getCreatedAt())
                        .build())
                .toList();
        List<BigInteger> referencedAssetFileIdList = getReferencedAssetFileIdList(triggerMessage);
        return AgentConversationContext.builder()
                .conversationId(conversationId)
                .triggerMessageId(triggerMessageId)
                .question(agentMention.removeFromContent(triggerMessage.getContent()).strip())
                .recentMessageList(contextMessageList)
                .receiverUserIdList(getReceiverUserIdList(conversationId))
                .quotedMessageId(triggerMessage.getQuoteMsgId())
                .referencedResourceIdList(referencedAssetFileIdList)
                .referencedAssetFileIdList(referencedAssetFileIdList)
                .build();
    }

    /**
     * 从触发消息和引用消息提取明确资产文件id.
     *
     * @param triggerMessage 触发消息
     * @return 去重后的资产文件id列表
     */
    private List<BigInteger> getReferencedAssetFileIdList(final Message triggerMessage) {
        List<Message> candidateMessageList = new ArrayList<>();
        candidateMessageList.add(triggerMessage);
        if (Objects.nonNull(triggerMessage.getQuoteMsgId())) {
            Message quotedMessage = messageDao.getById(triggerMessage.getQuoteMsgId());
            if (Objects.nonNull(quotedMessage)
                    && Objects.equals(quotedMessage.getConversationId(), triggerMessage.getConversationId())) {
                candidateMessageList.add(quotedMessage);
            }
        }
        return candidateMessageList.stream()
                .filter(message -> MessageTypeEnum.FILE.name().equals(message.getType()))
                .map(Message::getContent)
                .filter(Objects::nonNull)
                .map(BigInteger::new)
                .distinct()
                .toList();
    }

    /**
     * 查询当前用户的会话Agent权限.
     *
     * @param conversationId 会话id
     * @param userId         用户id
     * @return 当前会话Agent权限
     */
    @Override
    public AgentConversationPermission getPermission(
            final BigInteger conversationId,
            final BigInteger userId
    ) {
        if (Objects.isNull(conversationId) || Objects.isNull(userId)) {
            return AgentConversationPermission.nonMember();
        }
        Conversation conversation = conversationDao.getById(conversationId);
        if (Objects.isNull(conversation)
                || !conversationMemberDao.existsByConversationIdAndUserId(conversationId, userId)
                || !userConversationDao.existsAvailableByConversationIdAndUserId(conversationId, userId)) {
            return AgentConversationPermission.nonMember();
        }
        if (ConversationTypeEnum.PRIVATE.name().equals(conversation.getType())) {
            return AgentConversationPermission.builder()
                    .member(Boolean.TRUE)
                    .role(AgentConversationRoleEnum.MEMBER)
                    .conversationType(ConversationTypeEnum.PRIVATE)
                    .build();
        }
        if (!ConversationTypeEnum.GROUP.name().equals(conversation.getType())
                || Objects.isNull(conversation.getGroupId())) {
            return AgentConversationPermission.nonMember();
        }
        return userGroupMemberPermissionGrpcClient.getPermission(conversation.getGroupId(), userId);
    }

    /**
     * 查询消息发送者显示名称.
     *
     * @param message     消息实体
     * @param userInfoMap 用户信息映射
     * @return 发送者显示名称
     */
    private String getSenderDisplayName(
            final Message message,
            final Map<BigInteger, ConversationUserInfoRespVO> userInfoMap
    ) {
        if (MessageSenderTypeEnum.AGENT.name().equals(message.getSenderType())) {
            return MessageAgentConstant.DEFAULT_DISPLAY_NAME;
        }
        if (MessageSenderTypeEnum.SYSTEM.name().equals(message.getSenderType())) {
            return MessageAgentConstant.SYSTEM_DISPLAY_NAME;
        }
        ConversationUserInfoRespVO userInfo = userInfoMap.get(message.getSenderId());
        return Objects.nonNull(userInfo) && Objects.nonNull(userInfo.getUsername())
                ? userInfo.getUsername()
                : MessageAgentConstant.DEFAULT_USER_DISPLAY_NAME;
    }

    /**
     * 查询当前有效会话成员id列表.
     *
     * @param conversationId 会话id
     * @return 当前有效会话成员id列表
     */
    private List<BigInteger> getReceiverUserIdList(final BigInteger conversationId) {
        Set<BigInteger> availableUserIdSet = Set.copyOf(
                userConversationDao.getAvailableUserIdListByConversationId(conversationId)
        );
        return conversationMemberDao.getConversationMemberListByConversationId(conversationId)
                .stream()
                .map(ConversationMember::getUserId)
                .filter(availableUserIdSet::contains)
                .distinct()
                .toList();
    }
}
