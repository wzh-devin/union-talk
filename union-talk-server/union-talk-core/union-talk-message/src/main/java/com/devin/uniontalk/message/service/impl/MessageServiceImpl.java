package com.devin.uniontalk.message.service.impl;

import com.devin.uniontalk.base.cursor.model.CursorPageQuery;
import com.devin.uniontalk.base.cursor.model.CursorPageResult;
import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.infrastructure.file.enums.AssetFileTypeEnum;
import com.devin.uniontalk.infrastructure.message.constant.MessageMentionConstant;
import com.devin.uniontalk.infrastructure.message.enums.MessageMentionTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageOutboxAggregateTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageOutboxEventTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import com.devin.uniontalk.message.dao.ConversationDao;
import com.devin.uniontalk.message.dao.ConversationMemberDao;
import com.devin.uniontalk.message.dao.MessageDao;
import com.devin.uniontalk.message.dao.MessageMentionDao;
import com.devin.uniontalk.message.dao.MessageOutboxDao;
import com.devin.uniontalk.message.dao.UserConversationDao;
import com.devin.uniontalk.message.domain.command.MessageMentionCommand;
import com.devin.uniontalk.message.domain.entity.Conversation;
import com.devin.uniontalk.message.domain.entity.ConversationMember;
import com.devin.uniontalk.message.domain.entity.Message;
import com.devin.uniontalk.message.domain.entity.MessageMention;
import com.devin.uniontalk.message.domain.entity.MessageOutbox;
import com.devin.uniontalk.message.domain.entity.convertor.MessageConvertor;
import com.devin.uniontalk.message.domain.vo.resp.ConversationUserInfoRespVO;
import com.devin.uniontalk.message.domain.vo.resp.MessageAssetInfoRespVO;
import com.devin.uniontalk.message.domain.vo.resp.MessageRespVO;
import com.devin.uniontalk.message.grpc.client.FileAssetInfoGrpcClient;
import com.devin.uniontalk.message.grpc.client.UserConversationTargetInfoGrpcClient;
import com.devin.uniontalk.message.handler.MessageHandler;
import com.devin.uniontalk.message.handler.MessageHandlerRouter;
import com.devin.uniontalk.message.mq.factory.MessageEventFactory;
import com.devin.uniontalk.message.service.MessageAgentResponseAssembler;
import com.devin.uniontalk.message.service.MessageService;
import com.devin.uniontalk.rabbitmq.domain.event.AgentMentionedEvent;
import com.devin.uniontalk.rabbitmq.domain.event.ConversationUpdatedEvent;
import com.devin.uniontalk.rabbitmq.domain.event.MessageCreatedEvent;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2026/05/20 15:20:24.
 *
 * <p>
 * 消息表(Message)ServiceImpl层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    /**
     * 资产消息类型名称集合.
     */
    private static final Set<String> ASSET_MESSAGE_TYPE_NAME_SET = Set.of(
            MessageTypeEnum.AUDIO.name(),
            MessageTypeEnum.VIDEO.name(),
            MessageTypeEnum.FILE.name()
    );

    /**
     * 消息 Dao.
     */
    private final MessageDao messageDao;

    /**
     * 消息提及Dao.
     */
    private final MessageMentionDao messageMentionDao;

    /**
     * 消息事件发件箱Dao.
     */
    private final MessageOutboxDao messageOutboxDao;

    /**
     * 消息执行器路由器.
     */
    private final MessageHandlerRouter messageHandlerRouter;

    /**
     * 会话 Dao.
     */
    private final ConversationDao conversationDao;

    /**
     * 会话成员 Dao.
     */
    private final ConversationMemberDao conversationMemberDao;

    /**
     * 用户会话 Dao.
     */
    private final UserConversationDao userConversationDao;

    /**
     * 用户会话目标信息Grpc客户端.
     */
    private final UserConversationTargetInfoGrpcClient userConversationTargetInfoGrpcClient;

    /**
     * 文件资产信息Grpc客户端.
     */
    private final FileAssetInfoGrpcClient fileAssetInfoGrpcClient;

    /**
     * 消息事件快照工厂.
     */
    private final MessageEventFactory messageEventFactory;

    /**
     * 消息Agent响应组装器.
     */
    private final MessageAgentResponseAssembler messageAgentResponseAssembler;

    /**
     * 发送消息.
     *
     * @param senderId          发送者id
     * @param conversationId    会话id
     * @param type              消息类型
     * @param content           消息内容
     * @param quoteMsgId        引用消息id
     * @param mentionCommandList 结构化提及列表
     * @return 消息响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageRespVO send(
            final BigInteger senderId,
            final BigInteger conversationId,
            final MessageTypeEnum type,
            final String content,
            final BigInteger quoteMsgId,
            final List<MessageMentionCommand> mentionCommandList
    ) {
        AssertUtils.nonNull(senderId, BizErrorEnum.USER_NOT_FOUND);
        AssertUtils.nonNull(conversationId, BizErrorEnum.CONVERSATION_NOT_FOUND);
        MessageHandler messageHandler = messageHandlerRouter.route(type);
        messageHandler.handle(content);
        Conversation conversation = conversationDao.getById(conversationId);
        AssertUtils.nonNull(conversation, BizErrorEnum.CONVERSATION_NOT_FOUND);
        List<BigInteger> receiverUserIdList = getConversationUserIdList(conversationId);
        AssertUtils.isTrue(receiverUserIdList.contains(senderId), BizErrorEnum.NOT_CONVERSATION_MEMBER);
        validateMentionList(type, content, mentionCommandList, receiverUserIdList);
        MessageAssetInfoRespVO assetInfo = getSendAssetInfo(conversationId, messageHandler, content);

        // 保存消息，并使用消息创建时间作为会话最后消息时间。
        Message message = messageDao.createUserMessage(
                senderId,
                conversationId,
                type,
                content,
                quoteMsgId
        );
        List<MessageMention> mentionList = messageMentionDao.createMentionList(message, mentionCommandList);
        conversationDao.updateLastMessage(conversationId, message.getId(), message.getCreatedAt());

        // 更新所有成员会话展示时间，接收方未读数加一。
        userConversationDao.refreshMemberConversation(conversationId, receiverUserIdList);
        userConversationDao.increaseUnreadForReceivers(conversationId, senderId, receiverUserIdList);
        increaseMentionUnread(message, mentionList, receiverUserIdList, senderId);

        // 业务事务只写发件箱，由提交后的调度器可靠发布 RabbitMQ。
        MessageCreatedEvent messageCreatedEvent = messageEventFactory.createMessageCreatedEvent(
                message,
                mentionList,
                assetInfo,
                null,
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
        MessageMention agentMention = mentionList.stream()
                .filter(mention -> MessageMentionTypeEnum.AGENT.name().equals(mention.getMentionType()))
                .findFirst()
                .orElse(null);
        if (Objects.nonNull(agentMention)) {
            AgentMentionedEvent agentMentionedEvent =
                    messageEventFactory.createAgentMentionedEvent(messageCreatedEvent, agentMention.getTargetId());
            MessageOutbox event = messageOutboxDao.createEvent(
                    agentMentionedEvent.getEventId(),
                    MessageOutboxAggregateTypeEnum.MESSAGE,
                    message.getId(),
                    MessageOutboxEventTypeEnum.AGENT_MENTIONED,
                    agentMentionedEvent
            );
            log.info(event.toString());
        }
        MessageRespVO respVO = MessageConvertor.INSTANCE.toRespVO(message);
        respVO.setAssetInfo(assetInfo);
        respVO.setMentionList(MessageConvertor.INSTANCE.toMentionRespVOList(mentionList));
        return respVO;
    }

    /**
     * 查询会话消息分页列表.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @param cursorQuery    游标分页查询参数
     * @return 消息分页列表
     */
    @Override
    public CursorPageResult<MessageRespVO, Date> pageByConversationId(
            final BigInteger userId,
            final BigInteger conversationId,
            final CursorPageQuery<Date> cursorQuery
    ) {
        Conversation conversation = conversationDao.getById(conversationId);
        AssertUtils.nonNull(conversation, BizErrorEnum.CONVERSATION_NOT_FOUND);
        AssertUtils.isTrue(
                conversationMemberDao.existsByConversationIdAndUserId(conversationId, userId),
                BizErrorEnum.NOT_CONVERSATION_MEMBER
        );

        CursorPageResult<Message, Date> pageResult = messageDao.pageByConversationId(conversationId, cursorQuery);
        List<MessageRespVO> respVOList = MessageConvertor.INSTANCE.toRespVOList(pageResult.getList());
        fillMentionList(respVOList);
        fillSenderUserInfo(respVOList);
        messageAgentResponseAssembler.assemble(respVOList);
        fillAssetInfo(respVOList);
        return CursorPageResult.<MessageRespVO, Date>builder()
                .list(respVOList)
                .hasNext(pageResult.getHasNext())
                .nextCursorValue(pageResult.getNextCursorValue())
                .nextCursorId(pageResult.getNextCursorId())
                .build();
    }

    /**
     * 查询并校验待发送消息的资产文件信息.
     *
     * @param conversationId 会话id
     * @param messageHandler 消息执行器
     * @param content        消息内容
     * @return 消息资产文件信息，非资产消息返回null
     */
    private MessageAssetInfoRespVO getSendAssetInfo(
            final BigInteger conversationId,
            final MessageHandler messageHandler,
            final String content
    ) {
        Set<AssetFileTypeEnum> supportedAssetFileTypeSet = messageHandler.getSupportedAssetFileTypeSet();
        if (supportedAssetFileTypeSet.isEmpty()) {
            return null;
        }
        BigInteger assetId = new BigInteger(content);
        MessageAssetInfoRespVO assetInfo = fileAssetInfoGrpcClient.getAssetInfoMap(List.of(assetId)).get(assetId);
        AssertUtils.nonNull(assetInfo, BizErrorEnum.ASSET_FILE_NOT_FOUND);
        AssertUtils.equal(conversationId, assetInfo.getConversationId(), BizErrorEnum.ASSET_FILE_NOT_FOUND);

        AssertUtils.isTrue(
                supportedAssetFileTypeSet.contains(assetInfo.getFileType()),
                BizErrorEnum.MESSAGE_ASSET_TYPE_MISMATCH
        );
        return assetInfo;
    }

    /**
     * 批量填充消息资产文件信息.
     *
     * @param respVOList 消息响应列表
     */
    private void fillAssetInfo(final List<MessageRespVO> respVOList) {
        List<BigInteger> assetIdList = respVOList.stream()
                .filter(respVO -> ASSET_MESSAGE_TYPE_NAME_SET.contains(respVO.getType().name()))
                .map(MessageRespVO::getContent)
                .filter(Objects::nonNull)
                .filter(content -> !content.isEmpty() && content.chars().allMatch(Character::isDigit))
                .map(BigInteger::new)
                .distinct()
                .toList();
        if (assetIdList.isEmpty()) {
            return;
        }
        Map<BigInteger, MessageAssetInfoRespVO> assetInfoMap;
        try {
            assetInfoMap = fileAssetInfoGrpcClient.getAssetInfoMap(assetIdList);
        } catch (Exception e) {
            log.warn("批量填充消息资产文件信息失败, assetIdList={}", assetIdList, e);
            return;
        }
        respVOList.stream()
                .filter(respVO -> ASSET_MESSAGE_TYPE_NAME_SET.contains(respVO.getType().name()))
                .filter(respVO -> Objects.nonNull(respVO.getContent()))
                .filter(respVO -> !respVO.getContent().isEmpty()
                        && respVO.getContent().chars().allMatch(Character::isDigit))
                .forEach(respVO -> respVO.setAssetInfo(assetInfoMap.get(new BigInteger(respVO.getContent()))));
    }

    /**
     * 校验结构化提及与消息正文、类型和会话成员的一致性.
     *
     * @param type               消息类型
     * @param content            消息正文
     * @param mentionCommandList 结构化提及列表
     * @param receiverUserIdList 会话成员用户id列表
     */
    private void validateMentionList(
            final MessageTypeEnum type,
            final String content,
            final List<MessageMentionCommand> mentionCommandList,
            final List<BigInteger> receiverUserIdList
    ) {
        AssertUtils.nonNull(mentionCommandList, BizErrorEnum.MESSAGE_MENTION_PARAM_INVALID);
        AssertUtils.isTrue(
                mentionCommandList.size() <= MessageMentionConstant.MAX_COUNT,
                BizErrorEnum.MESSAGE_MENTION_PARAM_INVALID
        );
        AssertUtils.isFalse(
                !mentionCommandList.isEmpty() && !MessageTypeEnum.TEXT.equals(type),
                BizErrorEnum.MESSAGE_MENTION_TYPE_INVALID
        );
        AssertUtils.isTrue(
                mentionCommandList.stream().allMatch(Objects::nonNull),
                BizErrorEnum.MESSAGE_MENTION_PARAM_INVALID
        );
        List<MessageMentionCommand> sortedMentionList = mentionCommandList.stream()
                .sorted(Comparator.comparing(
                        MessageMentionCommand::getStartOffset,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .toList();
        int previousEndOffset = 0;
        int agentMentionCount = 0;
        for (MessageMentionCommand mention : sortedMentionList) {
            MessageMentionTypeEnum mentionType = mention.getMentionType();
            AssertUtils.nonNull(mentionType, BizErrorEnum.MESSAGE_MENTION_PARAM_INVALID);
            boolean validDisplayText = mention.getDisplayText() != null
                    && mention.getDisplayText().length() <= MessageMentionConstant.DISPLAY_TEXT_MAX_LENGTH;
            AssertUtils.isTrue(
                    validDisplayText
                            && mention.matchesContent(content)
                            && mention.getStartOffset() >= previousEndOffset,
                    BizErrorEnum.MESSAGE_MENTION_PARAM_INVALID
            );
            if (MessageMentionTypeEnum.USER.equals(mentionType)) {
                AssertUtils.isTrue(
                        receiverUserIdList.contains(mention.getTargetId()),
                        BizErrorEnum.MESSAGE_MENTION_TARGET_INVALID
                );
            } else if (MessageMentionTypeEnum.AGENT.equals(mentionType)) {
                boolean validAgentId = Objects.nonNull(mention.getTargetId())
                        && mention.getTargetId().signum() > 0
                        && mention.getTargetId().compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0;
                AssertUtils.isTrue(validAgentId, BizErrorEnum.MESSAGE_MENTION_TARGET_INVALID);
                agentMentionCount++;
            } else {
                AssertUtils.isTrue(
                        Objects.isNull(mention.getTargetId()),
                        BizErrorEnum.MESSAGE_MENTION_TARGET_INVALID
                );
            }
            previousEndOffset = mention.getEndOffset();
        }
        AssertUtils.isTrue(agentMentionCount <= 1, BizErrorEnum.MESSAGE_MENTION_PARAM_INVALID);
    }

    /**
     * 更新被提及会话成员的提及未读数.
     *
     * @param message            消息实体
     * @param mentionList        提及实体列表
     * @param receiverUserIdList 会话成员用户id列表
     * @param senderId           消息发送者id
     */
    private void increaseMentionUnread(
            final Message message,
            final List<MessageMention> mentionList,
            final List<BigInteger> receiverUserIdList,
            final BigInteger senderId
    ) {
        Set<BigInteger> mentionedUserIdSet = new HashSet<>();
        mentionList.forEach(mention -> {
            if (MessageMentionTypeEnum.USER.name().equals(mention.getMentionType())) {
                mentionedUserIdSet.add(mention.getTargetId());
            } else if (MessageMentionTypeEnum.ALL.name().equals(mention.getMentionType())) {
                mentionedUserIdSet.addAll(receiverUserIdList);
            }
        });
        mentionedUserIdSet.remove(senderId);
        mentionedUserIdSet.forEach(userId -> userConversationDao.increaseMentionUnread(
                userId,
                message.getConversationId(),
                message.getId()
        ));
    }

    /**
     * 批量填充消息提及响应列表.
     *
     * @param respVOList 消息响应列表
     */
    private void fillMentionList(final List<MessageRespVO> respVOList) {
        Map<BigInteger, List<MessageMention>> mentionListMap = messageMentionDao
                .getMentionListByMessageIds(respVOList.stream().map(MessageRespVO::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(MessageMention::getMessageId));
        respVOList.forEach(respVO -> respVO.setMentionList(MessageConvertor.INSTANCE.toMentionRespVOList(
                mentionListMap.getOrDefault(respVO.getId(), List.of())
        )));
    }

    /**
     * 批量填充消息发送者用户信息.
     *
     * @param respVOList 消息响应列表
     */
    private void fillSenderUserInfo(final List<MessageRespVO> respVOList) {
        if (respVOList.isEmpty()) {
            return;
        }
        List<BigInteger> senderIdList = getSenderIdList(respVOList);
        if (senderIdList.isEmpty()) {
            return;
        }
        UserConversationTargetInfoGrpcClient.TargetInfo targetInfo =
                userConversationTargetInfoGrpcClient.getConversationTargetInfo(senderIdList, List.of());
        Map<BigInteger, ConversationUserInfoRespVO> userInfoMap = targetInfo.getUserInfoMap();
        respVOList.forEach(respVO -> respVO.setSenderUser(userInfoMap.get(respVO.getSenderId())));
    }

    /**
     * 查询消息发送者id列表.
     *
     * @param respVOList 消息响应列表
     * @return 发送者id列表
     */
    private List<BigInteger> getSenderIdList(final List<MessageRespVO> respVOList) {
        return respVOList.stream()
                .map(MessageRespVO::getSenderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 查询会话内用户id列表.
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
                .filter(availableUserIdSet::contains)
                .distinct()
                .toList();
    }
}
