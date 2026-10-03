package com.devin.uniontalk.message.service.impl;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.infrastructure.message.constant.MessageAgentConstant;
import com.devin.uniontalk.infrastructure.message.enums.MessageOutboxAggregateTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageOutboxEventTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageMentionTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageSenderTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import com.devin.uniontalk.message.dao.ConversationDao;
import com.devin.uniontalk.message.dao.ConversationMemberDao;
import com.devin.uniontalk.message.dao.MessageAgentExtensionDao;
import com.devin.uniontalk.message.dao.MessageDao;
import com.devin.uniontalk.message.dao.MessageMentionDao;
import com.devin.uniontalk.message.dao.MessageOutboxDao;
import com.devin.uniontalk.message.dao.UserConversationDao;
import com.devin.uniontalk.message.domain.command.AgentReplyCommand;
import com.devin.uniontalk.message.domain.command.MessageMentionCommand;
import com.devin.uniontalk.message.domain.entity.Conversation;
import com.devin.uniontalk.message.domain.entity.ConversationMember;
import com.devin.uniontalk.message.domain.entity.Message;
import com.devin.uniontalk.message.domain.entity.MessageAgentExtension;
import com.devin.uniontalk.message.domain.entity.MessageMention;
import com.devin.uniontalk.message.domain.model.AgentCitation;
import com.devin.uniontalk.message.domain.model.AgentReplyResult;
import com.devin.uniontalk.message.domain.vo.resp.ConversationUserInfoRespVO;
import com.devin.uniontalk.message.grpc.client.UserConversationTargetInfoGrpcClient;
import com.devin.uniontalk.message.handler.MessageHandlerRouter;
import com.devin.uniontalk.message.mq.factory.MessageEventFactory;
import com.devin.uniontalk.message.service.AgentReplyCommandService;
import com.devin.uniontalk.rabbitmq.domain.event.ConversationUpdatedEvent;
import com.devin.uniontalk.rabbitmq.domain.event.MessageCreatedEvent;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 2026/07/28 23:30.
 *
 * <p>
 * Agent回复写回命令服务实现
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentReplyCommandServiceImpl implements AgentReplyCommandService {

    /**
     * 消息Dao.
     */
    private final MessageDao messageDao;

    /**
     * 消息提及Dao.
     */
    private final MessageMentionDao messageMentionDao;

    /**
     * AI消息扩展Dao.
     */
    private final MessageAgentExtensionDao messageAgentExtensionDao;

    /**
     * 消息发件箱Dao.
     */
    private final MessageOutboxDao messageOutboxDao;

    /**
     * 会话Dao.
     */
    private final ConversationDao conversationDao;

    /**
     * 会话成员Dao.
     */
    private final ConversationMemberDao conversationMemberDao;

    /**
     * 用户会话Dao.
     */
    private final UserConversationDao userConversationDao;

    /**
     * 消息执行器路由器.
     */
    private final MessageHandlerRouter messageHandlerRouter;

    /**
     * 消息事件快照工厂.
     */
    private final MessageEventFactory messageEventFactory;

    /**
     * 用户会话目标信息Grpc客户端.
     */
    private final UserConversationTargetInfoGrpcClient userConversationTargetInfoGrpcClient;

    /**
     * 创建Agent正式回复消息.
     *
     * @param command Agent回复写回命令
     * @return Agent回复写回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentReplyResult createAgentReply(final AgentReplyCommand command) {
        validateCommand(command);

        // 同一runId在事务内串行执行，代码层保证无唯一约束时仍可安全幂等。
        messageAgentExtensionDao.lockAgentRun(command.getRunId());
        MessageAgentExtension existingExtension =
                messageAgentExtensionDao.getByAgentRunId(command.getRunId());
        if (Objects.nonNull(existingExtension)) {
            return getExistingResult(command, existingExtension);
        }

        Message triggerMessage = messageDao.getByIdForUpdate(command.getTriggerMessageId());
        validateTriggerMessage(command, triggerMessage);
        Conversation conversation = conversationDao.getById(command.getConversationId());
        AssertUtils.nonNull(conversation, BizErrorEnum.CONVERSATION_NOT_FOUND);

        List<BigInteger> receiverUserIdList = getConversationUserIdList(command.getConversationId());
        AssertUtils.isFalse(receiverUserIdList.isEmpty(), BizErrorEnum.AGENT_REPLY_TRIGGER_INVALID);
        AssertUtils.isTrue(
                receiverUserIdList.contains(command.getReplyToUserId()),
                BizErrorEnum.AGENT_REPLY_TRIGGER_INVALID
        );
        ConversationUserInfoRespVO replyToUser = userConversationTargetInfoGrpcClient
                .getConversationTargetInfo(List.of(command.getReplyToUserId()), List.of())
                .getUserInfoMap()
                .get(command.getReplyToUserId());
        AssertUtils.isTrue(
                Objects.nonNull(replyToUser) && StringUtils.hasText(replyToUser.getUsername()),
                BizErrorEnum.AGENT_REPLY_TRIGGER_INVALID
        );
        String mentionDisplayText = "@" + replyToUser.getUsername();
        String answerContent = mentionDisplayText + "\n" + command.getContent();
        messageHandlerRouter.route(MessageTypeEnum.TEXT).handle(answerContent);

        Message answerMessage = messageDao.createAgentMessage(
                command.getConversationId(),
                answerContent,
                command.getTriggerMessageId()
        );
        List<MessageMention> mentionList = messageMentionDao.createMentionList(
                answerMessage,
                List.of(MessageMentionCommand.builder()
                        .mentionType(MessageMentionTypeEnum.USER)
                        .targetId(command.getReplyToUserId())
                        .displayText(mentionDisplayText)
                        .startOffset(0)
                        .length(mentionDisplayText.length())
                        .build())
        );
        MessageAgentExtension extension = messageAgentExtensionDao.create(answerMessage.getId(), command);
        conversationDao.updateLastMessage(
                conversation.getId(),
                answerMessage.getId(),
                answerMessage.getCreatedAt()
        );
        userConversationDao.refreshMemberConversation(conversation.getId(), receiverUserIdList);
        userConversationDao.increaseUnreadForAgentMessage(conversation.getId(), receiverUserIdList);
        userConversationDao.increaseMentionUnread(
                command.getReplyToUserId(),
                conversation.getId(),
                answerMessage.getId()
        );
        createOutboxEvents(conversation, answerMessage, mentionList, extension, receiverUserIdList);
        log.info(
                "创建Agent正式回复成功, runId={}, triggerMessageId={}, answerMessageId={}",
                command.getRunId(),
                command.getTriggerMessageId(),
                answerMessage.getId()
        );
        return AgentReplyResult.builder()
                .answerMessageId(answerMessage.getId())
                .created(Boolean.TRUE)
                .build();
    }

    /**
     * 校验Agent回复命令.
     *
     * @param command Agent回复写回命令
     */
    private void validateCommand(final AgentReplyCommand command) {
        AssertUtils.nonNull(command, BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        AssertUtils.isTrue(isDatabaseId(command.getRunId()), BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        AssertUtils.isTrue(isDatabaseId(command.getConversationId()), BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        AssertUtils.isTrue(isDatabaseId(command.getTriggerMessageId()), BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        AssertUtils.isTrue(isDatabaseId(command.getReplyToUserId()), BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        AssertUtils.isTrue(isDatabaseId(command.getAgentId()), BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        AssertUtils.notEmpty(command.getModelId(), BizErrorEnum.AGENT_REPLY_PARAM_INVALID);
        AssertUtils.isTrue(
                command.getModelId().length() <= MessageAgentConstant.MODEL_ID_MAX_LENGTH,
                BizErrorEnum.AGENT_REPLY_PARAM_INVALID
        );
        AssertUtils.isTrue(
                isValidCitationList(command.getCitationList()),
                BizErrorEnum.AGENT_REPLY_PARAM_INVALID
        );
        AssertUtils.equal(
                MessageAgentConstant.REPLY_IDEMPOTENCY_KEY_PREFIX + command.getRunId(),
                command.getIdempotencyKey(),
                BizErrorEnum.AGENT_REPLY_IDEMPOTENCY_KEY_INVALID
        );
    }

    /**
     * 校验触发消息.
     *
     * @param command        Agent回复写回命令
     * @param triggerMessage 触发消息
     */
    private void validateTriggerMessage(
            final AgentReplyCommand command,
            final Message triggerMessage
    ) {
        AssertUtils.nonNull(triggerMessage, BizErrorEnum.MESSAGE_NOT_FOUND);
        boolean valid = Objects.equals(command.getConversationId(), triggerMessage.getConversationId())
                && MessageSenderTypeEnum.USER.name().equals(triggerMessage.getSenderType())
                && MessageTypeEnum.TEXT.name().equals(triggerMessage.getType())
                && Objects.equals(command.getReplyToUserId(), triggerMessage.getSenderId())
                && messageMentionDao.existsAgentMention(triggerMessage.getId(), command.getAgentId())
                && !Boolean.TRUE.equals(triggerMessage.getRecalled())
                && Objects.isNull(triggerMessage.getDeletedAt())
                && Objects.nonNull(triggerMessage.getSenderId());
        AssertUtils.isTrue(valid, BizErrorEnum.AGENT_REPLY_TRIGGER_INVALID);
    }

    /**
     * 返回已经创建的Agent回复结果.
     *
     * @param command           Agent回复写回命令
     * @param existingExtension 已存在的AI消息扩展
     * @return Agent回复写回结果
     */
    private AgentReplyResult getExistingResult(
            final AgentReplyCommand command,
            final MessageAgentExtension existingExtension
    ) {
        Message existingMessage = messageDao.getById(existingExtension.getMessageId());
        boolean sameRequest = Objects.nonNull(existingMessage)
                && Objects.equals(command.getConversationId(), existingMessage.getConversationId())
                && MessageSenderTypeEnum.AGENT.name().equals(existingMessage.getSenderType())
                && MessageTypeEnum.TEXT.name().equals(existingMessage.getType())
                && Objects.isNull(existingMessage.getSenderId())
                && Objects.equals(existingExtension.getTriggerMessageId(), existingMessage.getQuoteMsgId())
                && Objects.equals(command.getTriggerMessageId(), existingExtension.getTriggerMessageId())
                && Objects.equals(command.getAgentId(), existingExtension.getAgentId())
                && messageMentionDao.getMentionListByMessageIds(List.of(existingMessage.getId()))
                        .stream()
                        .anyMatch(mention -> MessageMentionTypeEnum.USER.name().equals(mention.getMentionType())
                                && Objects.equals(command.getReplyToUserId(), mention.getTargetId()));
        AssertUtils.isTrue(sameRequest, BizErrorEnum.AGENT_REPLY_IDEMPOTENCY_KEY_INVALID);
        log.info(
                "复用已存在的Agent正式回复, runId={}, answerMessageId={}",
                command.getRunId(),
                existingExtension.getMessageId()
        );
        return AgentReplyResult.builder()
                .answerMessageId(existingExtension.getMessageId())
                .created(Boolean.FALSE)
                .build();
    }

    /**
     * 创建Agent回复的发件箱事件.
     *
     * @param conversation       会话
     * @param message            Agent回复消息
     * @param mentionList       结构化提及列表
     * @param extension          AI消息扩展
     * @param receiverUserIdList 接收用户id列表
     */
    private void createOutboxEvents(
            final Conversation conversation,
            final Message message,
            final List<MessageMention> mentionList,
            final MessageAgentExtension extension,
            final List<BigInteger> receiverUserIdList
    ) {
        MessageCreatedEvent messageCreatedEvent = messageEventFactory.createMessageCreatedEvent(
                message,
                mentionList,
                null,
                extension,
                receiverUserIdList
        );
        ConversationUpdatedEvent conversationUpdatedEvent = messageEventFactory.createConversationUpdatedEvent(
                conversation,
                message,
                receiverUserIdList
        );
        messageOutboxDao.createEvent(
                messageCreatedEvent.getEventId(),
                MessageOutboxAggregateTypeEnum.MESSAGE,
                message.getId(),
                MessageOutboxEventTypeEnum.MESSAGE_CREATED,
                messageCreatedEvent
        );
        messageOutboxDao.createEvent(
                conversationUpdatedEvent.getEventId(),
                MessageOutboxAggregateTypeEnum.CONVERSATION,
                conversation.getId(),
                MessageOutboxEventTypeEnum.CONVERSATION_UPDATED,
                conversationUpdatedEvent
        );
    }

    /**
     * 查询会话成员用户id列表.
     *
     * @param conversationId 会话id
     * @return 用户id列表
     */
    private List<BigInteger> getConversationUserIdList(final BigInteger conversationId) {
        Set<BigInteger> availableUserIdSet = Set.copyOf(
                userConversationDao.getAvailableUserIdListByConversationId(conversationId)
        );
        return conversationMemberDao.getConversationMemberListByConversationId(conversationId)
                .stream()
                .map(ConversationMember::getUserId)
                .filter(Objects::nonNull)
                .filter(availableUserIdSet::contains)
                .distinct()
                .toList();
    }

    /**
     * 判断id是否可存入数据库BIGINT字段.
     *
     * @param id id
     * @return 是否为有效数据库id
     */
    private boolean isDatabaseId(final BigInteger id) {
        return Objects.nonNull(id)
                && id.signum() > 0
                && id.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0;
    }

    /**
     * 判断Agent引用列表是否合法.
     *
     * @param citationList Agent引用列表
     * @return 是否合法
     */
    private boolean isValidCitationList(final List<AgentCitation> citationList) {
        if (Objects.isNull(citationList)) {
            return true;
        }
        if (citationList.size() > MessageAgentConstant.CITATION_MAX_COUNT
                || !citationList.stream().allMatch(this::isValidCitation)) {
            return false;
        }
        return citationList.stream()
                .map(AgentCitation::getCitationKey)
                .distinct()
                .count() == citationList.size();
    }

    /**
     * 判断Agent引用信息是否合法.
     *
     * @param citation Agent引用信息
     * @return 是否合法
     */
    private boolean isValidCitation(final AgentCitation citation) {
        if (Objects.isNull(citation)
                || !StringUtils.hasText(citation.getCitationKey())
                || citation.getCitationKey().length() > MessageAgentConstant.CITATION_KEY_MAX_LENGTH
                || Objects.isNull(citation.getSourceType())) {
            return false;
        }
        boolean validId = isOptionalDatabaseId(citation.getMessageId())
                && isOptionalDatabaseId(citation.getAssetFileId())
                && isOptionalDatabaseId(citation.getChunkId());
        boolean validNumber = isOptionalPositive(citation.getResourceVersion())
                && isOptionalPositive(citation.getPageFrom())
                && isOptionalPositive(citation.getPageTo());
        boolean validPageRange = Objects.isNull(citation.getPageFrom())
                || Objects.isNull(citation.getPageTo())
                || citation.getPageFrom() <= citation.getPageTo();
        boolean validHeadingPath = Objects.isNull(citation.getHeadingPath())
                || citation.getHeadingPath().length() <= MessageAgentConstant.CITATION_HEADING_PATH_MAX_LENGTH;
        boolean validSourceReference = switch (citation.getSourceType()) {
            case MESSAGE_SEGMENT -> Objects.nonNull(citation.getMessageId());
            case RESOURCE_CHUNK -> Objects.nonNull(citation.getAssetFileId())
                    && Objects.nonNull(citation.getResourceVersion())
                    && Objects.nonNull(citation.getChunkId());
        };
        return validId && validNumber && validPageRange && validHeadingPath && validSourceReference;
    }

    /**
     * 判断可选数据库id是否合法.
     *
     * @param id 可选id
     * @return 是否合法
     */
    private boolean isOptionalDatabaseId(final BigInteger id) {
        return Objects.isNull(id) || isDatabaseId(id);
    }

    /**
     * 判断可选整数是否为正数.
     *
     * @param value 可选整数
     * @return 是否合法
     */
    private boolean isOptionalPositive(final Integer value) {
        return Objects.isNull(value) || value > 0;
    }
}
