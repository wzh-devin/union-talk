package com.devin.uniontalk.message.service;

import com.devin.uniontalk.message.dao.MessageAgentExtensionDao;
import com.devin.uniontalk.message.domain.entity.MessageAgentExtension;
import com.devin.uniontalk.message.domain.entity.convertor.MessageConvertor;
import com.devin.uniontalk.message.domain.vo.resp.MessageRespVO;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 2026/07/29.
 *
 * <p>
 * 消息Agent响应组装器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class MessageAgentResponseAssembler {

    /**
     * AI消息扩展Dao.
     */
    private final MessageAgentExtensionDao messageAgentExtensionDao;

    /**
     * 批量组装消息Agent响应信息.
     *
     * @param messageList 消息响应列表
     */
    public void assemble(final List<MessageRespVO> messageList) {
        if (Objects.isNull(messageList) || messageList.isEmpty()) {
            return;
        }
        List<BigInteger> messageIdList = messageList.stream()
                .map(MessageRespVO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<BigInteger, MessageAgentExtension> extensionMap =
                messageAgentExtensionDao.getExtensionMapByMessageIdList(messageIdList);
        messageList.forEach(message -> assemble(message, extensionMap.get(message.getId())));
    }

    /**
     * 组装单条消息Agent响应信息.
     *
     * @param message   消息响应
     * @param extension AI消息扩展
     */
    private void assemble(
            final MessageRespVO message,
            final MessageAgentExtension extension
    ) {
        if (Objects.isNull(extension)) {
            return;
        }
        message.setAgentRunId(extension.getAgentRunId());
        message.setTriggerMessageId(extension.getTriggerMessageId());
        message.setSenderAgent(MessageConvertor.INSTANCE.toAgentInfoRespVO(extension));
        message.setCitationList(MessageConvertor.INSTANCE.toCitationRespVOList(extension.getCitationList()));
    }
}
