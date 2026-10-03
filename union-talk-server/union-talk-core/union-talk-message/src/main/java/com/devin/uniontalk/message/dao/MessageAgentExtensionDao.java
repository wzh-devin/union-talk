package com.devin.uniontalk.message.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.message.domain.command.AgentReplyCommand;
import com.devin.uniontalk.message.domain.entity.MessageAgentExtension;
import com.devin.uniontalk.message.mapper.MessageAgentExtensionMapper;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2026/07/28 22:15.
 *
 * <p>
 * AI消息扩展Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageAgentExtensionDao extends ServiceImpl<MessageAgentExtensionMapper, MessageAgentExtension> {

    /**
     * 获取Agent回复事务锁.
     *
     * @param agentRunId Agent运行id
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockAgentRun(final BigInteger agentRunId) {
        getBaseMapper().lockAgentRun(agentRunId);
    }

    /**
     * 根据Agent运行id查询扩展信息.
     *
     * @param agentRunId Agent运行id
     * @return AI消息扩展
     */
    public MessageAgentExtension getByAgentRunId(final BigInteger agentRunId) {
        return lambdaQuery()
                .eq(MessageAgentExtension::getAgentRunId, agentRunId)
                .last("ORDER BY created_at, message_id LIMIT 1")
                .one();
    }

    /**
     * 根据消息id列表查询扩展信息映射.
     *
     * @param messageIdList 消息id列表
     * @return 消息id与扩展信息映射
     */
    public Map<BigInteger, MessageAgentExtension> getExtensionMapByMessageIdList(
            final List<BigInteger> messageIdList
    ) {
        if (Objects.isNull(messageIdList) || messageIdList.isEmpty()) {
            return Map.of();
        }
        return lambdaQuery()
                .in(MessageAgentExtension::getMessageId, messageIdList)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        MessageAgentExtension::getMessageId,
                        Function.identity(),
                        (first, ignored) -> first
                ));
    }

    /**
     * 创建AI消息扩展.
     *
     * @param messageId AI回复消息id
     * @param command   Agent回复命令
     * @return AI消息扩展
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public MessageAgentExtension create(
            final BigInteger messageId,
            final AgentReplyCommand command
    ) {
        MessageAgentExtension extension = new MessageAgentExtension();
        extension.init(messageId, command);
        boolean saved = save(extension);
        if (!saved) {
            throw new IllegalStateException("创建AI消息扩展失败");
        }
        return extension;
    }
}
