package com.devin.uniontalk.message.service.impl;

import com.devin.uniontalk.base.cursor.model.CursorPageQuery;
import com.devin.uniontalk.base.cursor.model.CursorPageResult;
import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.infrastructure.message.enums.ConversationTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import com.devin.uniontalk.message.dao.ConversationDao;
import com.devin.uniontalk.message.dao.ConversationMemberDao;
import com.devin.uniontalk.message.dao.MessageDao;
import com.devin.uniontalk.message.dao.UserConversationDao;
import com.devin.uniontalk.message.domain.entity.Conversation;
import com.devin.uniontalk.message.domain.entity.ConversationMember;
import com.devin.uniontalk.message.domain.entity.Message;
import com.devin.uniontalk.message.domain.entity.UserConversation;
import com.devin.uniontalk.message.domain.entity.convertor.ConversationConvertor;
import com.devin.uniontalk.message.domain.entity.convertor.MessageConvertor;
import com.devin.uniontalk.message.domain.model.ConversationAssetContext;
import com.devin.uniontalk.message.domain.vo.resp.ConversationRespVO;
import com.devin.uniontalk.message.domain.vo.resp.MessageAssetInfoRespVO;
import com.devin.uniontalk.message.domain.vo.resp.MessageRespVO;
import com.devin.uniontalk.message.grpc.client.FileAssetInfoGrpcClient;
import com.devin.uniontalk.message.grpc.client.UserConversationTargetInfoGrpcClient;
import com.devin.uniontalk.message.service.ConversationService;
import com.devin.uniontalk.message.service.MessageAgentResponseAssembler;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2026/05/20 15:20:23.
 *
 * <p>
 *  会话表(Conversation)ServiceImpl层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    /**
     * 资产消息类型名称集合.
     */
    private static final Set<String> ASSET_MESSAGE_TYPE_NAME_SET = Set.of(
            MessageTypeEnum.AUDIO.name(),
            MessageTypeEnum.VIDEO.name(),
            MessageTypeEnum.FILE.name()
    );

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
     * 消息 Dao.
     */
    private final MessageDao messageDao;

    /**
     * 用户会话目标信息Grpc客户端.
     */
    private final UserConversationTargetInfoGrpcClient userConversationTargetInfoGrpcClient;

    /**
     * 文件资产信息Grpc客户端.
     */
    private final FileAssetInfoGrpcClient fileAssetInfoGrpcClient;

    /**
     * 消息Agent响应组装器.
     */
    private final MessageAgentResponseAssembler messageAgentResponseAssembler;

    /**
     * 创建私聊会话.
     *
     * @param fromUserId 发送方用户id
     * @param toUserId   接收方用户id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigInteger createPrivateConversation(final BigInteger fromUserId, final BigInteger toUserId) {
        AssertUtils.isTrue(
                Objects.nonNull(fromUserId) && Objects.nonNull(toUserId) && !Objects.equals(fromUserId, toUserId),
                BizErrorEnum.CONVERSATION_CREATE_FAILED
        );
        List<BigInteger> memberIdList = List.of(fromUserId, toUserId);
        Conversation existsConversation = getPrivateConversation(memberIdList);
        if (Objects.nonNull(existsConversation)) {
            log.info("私聊会话已存在, conversationId={}, memberIdList={}", existsConversation.getId(), memberIdList);
            return existsConversation.getId();
        }

        // Service 只编排创建顺序，具体实体初始化与保存收敛在 Dao。
        Conversation conversation = conversationDao.createConversation(ConversationTypeEnum.PRIVATE, null);
        conversationMemberDao.initConversationMembers(conversation.getId(), memberIdList);
        userConversationDao.initUserConversations(conversation.getId(), memberIdList);
        log.info("创建私聊会话成功, conversationId={}, memberIdList={}", conversation.getId(), memberIdList);
        return conversation.getId();
    }

    /**
     * 创建群聊会话.
     *
     * @param groupId      群聊id
     * @param memberIdList 群成员id列表
     * @return 会话id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigInteger createGroupConversation(final BigInteger groupId, final List<BigInteger> memberIdList) {
        AssertUtils.isTrue(
                Objects.nonNull(groupId) && Objects.nonNull(memberIdList) && !memberIdList.isEmpty(),
                BizErrorEnum.CONVERSATION_CREATE_FAILED
        );
        Conversation existsConversation = conversationDao.getGroupConversation(groupId);
        if (Objects.nonNull(existsConversation)) {
            log.info("群聊会话已存在, conversationId={}, groupId={}", existsConversation.getId(), groupId);
            return existsConversation.getId();
        }

        // 群聊会话通过 groupId 幂等识别，成员来自 user 模块同步 Grpc 调用。
        Conversation conversation = conversationDao.createConversation(ConversationTypeEnum.GROUP, groupId);
        conversationMemberDao.initConversationMembers(conversation.getId(), memberIdList);
        userConversationDao.initUserConversations(conversation.getId(), memberIdList);
        log.info("创建群聊会话成功, conversationId={}, groupId={}", conversation.getId(), groupId);
        return conversation.getId();
    }

    /**
     * 解散群聊关联会话.
     *
     * @param groupId 群聊id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroupConversation(final BigInteger groupId) {
        AssertUtils.nonNull(groupId, BizErrorEnum.CONVERSATION_DISSOLVE_FAILED);
        Conversation conversation = conversationDao.getGroupConversation(groupId);
        AssertUtils.nonNull(conversation, BizErrorEnum.CONVERSATION_NOT_FOUND);
        userConversationDao.dissolveByConversationId(conversation.getId());
        log.info("解散群聊关联会话成功, groupId={}, conversationId={}", groupId, conversation.getId());
    }

    /**
     * 查询当前用户会话分页列表.
     *
     * @param userId      用户id
     * @param cursorQuery 游标分页查询参数
     * @return 会话分页列表
     */
    @Override
    public CursorPageResult<ConversationRespVO, Date> pageByUserId(
            final BigInteger userId,
            final CursorPageQuery<Date> cursorQuery
    ) {
        CursorPageResult<UserConversation, Date> pageResult = userConversationDao.pageByUserId(userId, cursorQuery);
        List<UserConversation> userConversationList = pageResult.getList();
        if (userConversationList.isEmpty()) {
            return CursorPageResult.<ConversationRespVO, Date>builder()
                    .list(List.of())
                    .hasNext(Boolean.FALSE)
                    .build();
        }

        // 批量查询会话与最后一条消息，避免列表接口产生逐条查询。
        Map<BigInteger, Conversation> conversationMap = getConversationMap(userConversationList);
        Map<BigInteger, Message> messageMap = getLastMessageMap(conversationMap.values().stream().toList());
        List<ConversationRespVO> respVOList = userConversationList.stream()
                .map(userConversation -> toConversationRespVO(userConversation, conversationMap, messageMap))
                .filter(Objects::nonNull)
                .toList();
        fillConversationTargetInfo(userId, conversationMap, respVOList);
        fillLastMessageAgentInfo(respVOList);
        fillLastMessageAssetInfo(respVOList);
        return CursorPageResult.<ConversationRespVO, Date>builder()
                .list(respVOList)
                .hasNext(pageResult.getHasNext())
                .nextCursorValue(pageResult.getNextCursorValue())
                .nextCursorId(pageResult.getNextCursorId())
                .build();
    }

    /**
     * 查询当前用户会话详情.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @return 会话详情
     */
    @Override
    public ConversationRespVO getDetail(final BigInteger userId, final BigInteger conversationId) {
        Conversation conversation = conversationDao.getById(conversationId);
        AssertUtils.nonNull(conversation, BizErrorEnum.CONVERSATION_NOT_FOUND);
        validateConversationMember(conversationId, userId);
        UserConversation userConversation = userConversationDao.getByUserIdAndConversationId(userId, conversationId);
        AssertUtils.nonNull(userConversation, BizErrorEnum.NOT_CONVERSATION_MEMBER);
        Message lastMessage = getLastMessage(conversation);
        ConversationRespVO respVO = toConversationRespVO(conversation, userConversation, lastMessage);
        fillConversationTargetInfo(userId, Map.of(conversation.getId(), conversation), List.of(respVO));
        fillLastMessageAgentInfo(List.of(respVO));
        fillLastMessageAssetInfo(List.of(respVO));
        return respVO;
    }

    /**
     * 批量填充会话最后消息的Agent信息.
     *
     * @param respVOList 会话响应列表
     */
    private void fillLastMessageAgentInfo(final List<ConversationRespVO> respVOList) {
        List<MessageRespVO> lastMessageList = respVOList.stream()
                .map(ConversationRespVO::getLastMessage)
                .filter(Objects::nonNull)
                .toList();
        messageAgentResponseAssembler.assemble(lastMessageList);
    }

    /**
     * 批量填充会话最后消息的资产文件信息.
     *
     * @param respVOList 会话响应列表
     */
    private void fillLastMessageAssetInfo(final List<ConversationRespVO> respVOList) {
        List<MessageRespVO> lastMessageList = respVOList.stream()
                .map(ConversationRespVO::getLastMessage)
                .filter(Objects::nonNull)
                .filter(message -> ASSET_MESSAGE_TYPE_NAME_SET.contains(message.getType()))
                .toList();
        List<BigInteger> assetIdList = lastMessageList.stream()
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
            log.warn("批量填充会话最后消息资产文件信息失败, assetIdList={}", assetIdList, e);
            return;
        }
        lastMessageList.stream()
                .filter(message -> Objects.nonNull(message.getContent()))
                .filter(message -> !message.getContent().isEmpty()
                        && message.getContent().chars().allMatch(Character::isDigit))
                .forEach(message -> message.setAssetInfo(
                        assetInfoMap.get(new BigInteger(message.getContent()))
                ));
    }

    /**
     * 标记会话已读.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @param lastReadMsgId  最后已读消息id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void read(final BigInteger userId, final BigInteger conversationId, final BigInteger lastReadMsgId) {
        Conversation conversation = conversationDao.getById(conversationId);
        AssertUtils.nonNull(conversation, BizErrorEnum.CONVERSATION_NOT_FOUND);
        validateConversationMember(conversationId, userId);
        UserConversation userConversation = userConversationDao.getByUserIdAndConversationId(userId, conversationId);
        AssertUtils.nonNull(userConversation, BizErrorEnum.NOT_CONVERSATION_MEMBER);

        // 未传已读消息id时，默认标记到会话最后一条消息。
        BigInteger targetLastReadMsgId = Objects.nonNull(lastReadMsgId) ? lastReadMsgId : conversation.getLastMsgId();
        userConversationDao.clearUnread(userId, conversationId, targetLastReadMsgId);
    }

    /**
     * 判断用户是否属于会话.
     *
     * @param conversationId 会话id
     * @param userId         用户id
     * @return 是否属于会话
     */
    @Override
    public boolean existsConversationMember(final BigInteger conversationId, final BigInteger userId) {
        if (Objects.isNull(conversationId) || Objects.isNull(userId)) {
            return false;
        }
        Conversation conversation = conversationDao.getById(conversationId);
        if (Objects.isNull(conversation)) {
            return false;
        }
        return conversationMemberDao.existsByConversationIdAndUserId(conversationId, userId);
    }

    /**
     * 查询会话资产上下文.
     *
     * @param conversationId 会话id
     * @param operatorUserId 操作用户id
     * @return 会话资产上下文
     */
    @Override
    public ConversationAssetContext getConversationAssetContext(
            final BigInteger conversationId,
            final BigInteger operatorUserId
    ) {
        Conversation conversation = conversationDao.getById(conversationId);
        AssertUtils.nonNull(conversation, BizErrorEnum.CONVERSATION_NOT_FOUND);
        boolean privateMember = ConversationTypeEnum.PRIVATE.name().equals(conversation.getType())
                && conversationMemberDao.existsByConversationIdAndUserId(conversationId, operatorUserId);
        return ConversationAssetContext.builder()
                .conversationId(conversation.getId())
                .conversationType(ConversationTypeEnum.valueOf(conversation.getType()))
                .groupId(conversation.getGroupId())
                .privateMember(privateMember)
                .build();
    }

    /**
     * 校验用户属于会话.
     *
     * @param conversationId 会话id
     * @param userId         用户id
     */
    private void validateConversationMember(final BigInteger conversationId, final BigInteger userId) {
        AssertUtils.isTrue(
                conversationMemberDao.existsByConversationIdAndUserId(conversationId, userId),
                BizErrorEnum.NOT_CONVERSATION_MEMBER
        );
    }

    /**
     * 查询已存在的私聊会话.
     *
     * @param memberIdList 成员id列表
     * @return 私聊会话
     */
    private Conversation getPrivateConversation(final List<BigInteger> memberIdList) {
        List<BigInteger> conversationIdList = conversationMemberDao.getCommonConversationIdList(memberIdList);
        if (conversationIdList.isEmpty()) {
            return null;
        }
        return conversationDao.getConversationListByIds(conversationIdList)
                .stream()
                .filter(conversation -> ConversationTypeEnum.PRIVATE.name().equals(conversation.getType()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查询会话映射.
     *
     * @param userConversationList 用户会话列表
     * @return 会话映射
     */
    private Map<BigInteger, Conversation> getConversationMap(final List<UserConversation> userConversationList) {
        List<BigInteger> conversationIdList = userConversationList.stream()
                .map(UserConversation::getConversationId)
                .distinct()
                .toList();
        return conversationDao.getConversationListByIds(conversationIdList)
                .stream()
                .collect(Collectors.toMap(Conversation::getId, Function.identity()));
    }

    /**
     * 查询最后一条消息映射.
     *
     * @param conversationList 会话列表
     * @return 消息映射
     */
    private Map<BigInteger, Message> getLastMessageMap(final List<Conversation> conversationList) {
        List<BigInteger> lastMsgIdList = conversationList.stream()
                .map(Conversation::getLastMsgId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (lastMsgIdList.isEmpty()) {
            return Map.of();
        }
        return messageDao.getMessageListByIds(lastMsgIdList)
                .stream()
                .collect(Collectors.toMap(Message::getId, Function.identity()));
    }

    /**
     * 填充会话目标信息.
     *
     * @param userId          当前用户id
     * @param conversationMap 会话映射
     * @param respVOList      会话响应列表
     */
    private void fillConversationTargetInfo(
            final BigInteger userId,
            final Map<BigInteger, Conversation> conversationMap,
            final List<ConversationRespVO> respVOList
    ) {
        if (respVOList.isEmpty()) {
            return;
        }
        List<Conversation> conversationList = respVOList.stream()
                .map(respVO -> conversationMap.get(respVO.getConversationId()))
                .filter(Objects::nonNull)
                .toList();
        Map<BigInteger, BigInteger> targetUserIdMap = getTargetUserIdMap(userId, conversationList);
        List<BigInteger> targetUserIdList = targetUserIdMap.values()
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<BigInteger> groupIdList = getGroupIdList(conversationList);
        if (targetUserIdList.isEmpty() && groupIdList.isEmpty()) {
            return;
        }

        UserConversationTargetInfoGrpcClient.TargetInfo targetInfo =
                userConversationTargetInfoGrpcClient.getConversationTargetInfo(targetUserIdList, groupIdList);
        respVOList.forEach(respVO -> fillConversationTargetInfo(respVO, targetUserIdMap, targetInfo));
    }

    /**
     * 填充单个会话目标信息.
     *
     * @param respVO          会话响应参数
     * @param targetUserIdMap 目标用户id映射
     * @param targetInfo      会话目标信息
     */
    private void fillConversationTargetInfo(
            final ConversationRespVO respVO,
            final Map<BigInteger, BigInteger> targetUserIdMap,
            final UserConversationTargetInfoGrpcClient.TargetInfo targetInfo
    ) {
        if (ConversationTypeEnum.PRIVATE == respVO.getType()) {
            BigInteger targetUserId = targetUserIdMap.get(respVO.getConversationId());
            respVO.setTargetUser(targetInfo.getUserInfoMap().get(targetUserId));
            return;
        }
        if (ConversationTypeEnum.GROUP == respVO.getType()) {
            respVO.setGroupInfo(targetInfo.getGroupInfoMap().get(respVO.getGroupId()));
        }
    }

    /**
     * 查询私聊目标用户id映射.
     *
     * @param userId           当前用户id
     * @param conversationList 会话列表
     * @return 私聊目标用户id映射
     */
    private Map<BigInteger, BigInteger> getTargetUserIdMap(
            final BigInteger userId,
            final List<Conversation> conversationList
    ) {
        List<BigInteger> privateConversationIdList = getPrivateConversationIdList(conversationList);
        if (privateConversationIdList.isEmpty()) {
            return Map.of();
        }
        Map<BigInteger, List<BigInteger>> targetUserIdListMap = conversationMemberDao
                .getConversationMemberListByConversationIds(privateConversationIdList)
                .stream()
                .filter(member -> !Objects.equals(userId, member.getUserId()))
                .collect(Collectors.groupingBy(
                        ConversationMember::getConversationId,
                        Collectors.mapping(ConversationMember::getUserId, Collectors.toList())
                ));
        return privateConversationIdList.stream()
                .map(conversationId -> new AbstractMap.SimpleEntry<>(
                        conversationId,
                        getTargetUserId(conversationId, targetUserIdListMap.get(conversationId))
                ))
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 查询私聊会话id列表.
     *
     * @param conversationList 会话列表
     * @return 私聊会话id列表
     */
    private List<BigInteger> getPrivateConversationIdList(final List<Conversation> conversationList) {
        return conversationList.stream()
                .filter(conversation -> ConversationTypeEnum.PRIVATE.name().equals(conversation.getType()))
                .map(Conversation::getId)
                .distinct()
                .toList();
    }

    /**
     * 查询群聊id列表.
     *
     * @param conversationList 会话列表
     * @return 群聊id列表
     */
    private List<BigInteger> getGroupIdList(final List<Conversation> conversationList) {
        return conversationList.stream()
                .filter(conversation -> ConversationTypeEnum.GROUP.name().equals(conversation.getType()))
                .map(Conversation::getGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 获取私聊目标用户id.
     *
     * @param conversationId   会话id
     * @param targetUserIdList 目标用户id列表
     * @return 目标用户id
     */
    private BigInteger getTargetUserId(
            final BigInteger conversationId,
            final List<BigInteger> targetUserIdList
    ) {
        if (Objects.isNull(targetUserIdList) || targetUserIdList.isEmpty()) {
            log.warn("私聊会话缺少目标用户, conversationId={}", conversationId);
            return null;
        }
        if (targetUserIdList.size() > 1) {
            log.warn("私聊会话存在多个目标用户, conversationId={}, targetUserIdList={}", conversationId, targetUserIdList);
        }
        return targetUserIdList.getFirst();
    }

    /**
     * 转换会话响应参数.
     *
     * @param userConversation 用户会话实体
     * @param conversationMap  会话映射
     * @param messageMap       消息映射
     * @return 会话响应参数
     */
    private ConversationRespVO toConversationRespVO(
            final UserConversation userConversation,
            final Map<BigInteger, Conversation> conversationMap,
            final Map<BigInteger, Message> messageMap
    ) {
        Conversation conversation = conversationMap.get(userConversation.getConversationId());
        if (Objects.isNull(conversation)) {
            return null;
        }
        Message lastMessage = Objects.nonNull(conversation.getLastMsgId())
                ? messageMap.get(conversation.getLastMsgId())
                : null;
        return toConversationRespVO(conversation, userConversation, lastMessage);
    }

    /**
     * 转换会话响应参数.
     *
     * @param conversation     会话实体
     * @param userConversation 用户会话实体
     * @param lastMessage      最后一条消息实体
     * @return 会话响应参数
     */
    private ConversationRespVO toConversationRespVO(
            final Conversation conversation,
            final UserConversation userConversation,
            final Message lastMessage
    ) {
        ConversationRespVO respVO = ConversationConvertor.INSTANCE.toRespVO(conversation, userConversation);
        respVO.setLastMessage(MessageConvertor.INSTANCE.toRespVO(lastMessage));
        return respVO;
    }

    /**
     * 获取会话最后一条消息.
     *
     * @param conversation 会话实体
     * @return 最后一条消息
     */
    private Message getLastMessage(final Conversation conversation) {
        if (Objects.isNull(conversation.getLastMsgId())) {
            return null;
        }
        return messageDao.getById(conversation.getLastMsgId());
    }
}
