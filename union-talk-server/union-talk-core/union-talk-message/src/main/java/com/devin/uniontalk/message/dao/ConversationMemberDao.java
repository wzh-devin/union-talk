package com.devin.uniontalk.message.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.message.domain.entity.ConversationMember;
import com.devin.uniontalk.message.mapper.ConversationMemberMapper;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 2026/05/20 15:20:24.
 *
 * <p>
 *  私聊会话成员(ConversationMember)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemberDao extends ServiceImpl<ConversationMemberMapper, ConversationMember> {

    /**
     * 初始化会话成员列表.
     *
     * @param conversationId 会话id
     * @param memberIdList   成员id列表
     */
    public void initConversationMembers(final BigInteger conversationId, final List<BigInteger> memberIdList) {
        if (Objects.isNull(memberIdList) || memberIdList.isEmpty()) {
            return;
        }
        saveBatch(memberIdList.stream()
                .map(memberId -> buildConversationMember(conversationId, memberId))
                .toList());
    }

    /**
     * 查询会话成员列表.
     *
     * @param conversationId 会话id
     * @return 会话成员列表
     */
    public List<ConversationMember> getConversationMemberListByConversationId(final BigInteger conversationId) {
        return lambdaQuery()
                .eq(ConversationMember::getConversationId, conversationId)
                .list();
    }

    /**
     * 批量查询会话成员列表.
     *
     * @param idList 会话id列表
     * @return 会话成员列表
     */
    public List<ConversationMember> getConversationMemberListByConversationIds(final List<BigInteger> idList) {
        if (Objects.isNull(idList) || idList.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(ConversationMember::getConversationId, idList)
                .list();
    }

    /**
     * 查询用户所在会话成员记录.
     *
     * @param userId 用户id
     * @return 会话成员列表
     */
    public List<ConversationMember> getConversationMemberListByUserId(final BigInteger userId) {
        return lambdaQuery()
                .eq(ConversationMember::getUserId, userId)
                .list();
    }

    /**
     * 判断用户是否属于会话.
     *
     * @param conversationId 会话id
     * @param userId         用户id
     * @return 是否属于会话
     */
    public boolean existsByConversationIdAndUserId(final BigInteger conversationId, final BigInteger userId) {
        return lambdaQuery()
                .eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getUserId, userId)
                .exists();
    }

    /**
     * 查询共同会话id列表.
     *
     * @param userIdList 用户id列表
     * @return 共同会话id列表
     */
    public List<BigInteger> getCommonConversationIdList(final List<BigInteger> userIdList) {
        if (Objects.isNull(userIdList) || userIdList.isEmpty()) {
            return List.of();
        }
        int userCount = userIdList.size();
        return lambdaQuery()
                .in(ConversationMember::getUserId, userIdList)
                .list()
                .stream()
                .collect(Collectors.groupingBy(ConversationMember::getConversationId))
                .entrySet()
                .stream()
                .filter(entry -> getUserIdSet(entry.getValue()).containsAll(userIdList))
                .filter(entry -> entry.getValue().size() == userCount)
                .map(entry -> entry.getKey())
                .toList();
    }

    /**
     * 转换会话成员用户id集合.
     *
     * @param memberList 会话成员列表
     * @return 用户id集合
     */
    private Set<BigInteger> getUserIdSet(final List<ConversationMember> memberList) {
        return memberList.stream()
                .map(ConversationMember::getUserId)
                .collect(Collectors.toSet());
    }

    /**
     * 构建会话成员实体.
     *
     * @param conversationId 会话id
     * @param userId         用户id
     * @return 会话成员实体
     */
    private ConversationMember buildConversationMember(final BigInteger conversationId, final BigInteger userId) {
        ConversationMember conversationMember = new ConversationMember();
        conversationMember.setId(IdGenerator.nextIdBigInteger());
        conversationMember.setConversationId(conversationId);
        conversationMember.setUserId(userId);
        conversationMember.init();
        return conversationMember;
    }
}
