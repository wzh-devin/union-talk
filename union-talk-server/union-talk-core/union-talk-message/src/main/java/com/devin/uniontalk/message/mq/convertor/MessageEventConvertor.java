package com.devin.uniontalk.message.mq.convertor;

import com.devin.uniontalk.infrastructure.message.constant.MessageAgentConstant;
import com.devin.uniontalk.message.domain.entity.MessageAgentExtension;
import com.devin.uniontalk.message.domain.entity.MessageMention;
import com.devin.uniontalk.message.domain.model.AgentCitation;
import com.devin.uniontalk.message.domain.vo.resp.ConversationUserInfoRespVO;
import com.devin.uniontalk.message.domain.vo.resp.MessageAssetInfoRespVO;
import com.devin.uniontalk.rabbitmq.domain.event.MessageCreatedEvent;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 2026/08/07.
 *
 * <p>
 * 消息事件快照转换器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper(
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MessageEventConvertor {

    /**
     * 消息事件快照转换器实例.
     */
    MessageEventConvertor INSTANCE = Mappers.getMapper(MessageEventConvertor.class);

    /**
     * 将消息提及实体转换为事件快照.
     *
     * @param mention 消息提及实体
     * @return 消息提及事件快照
     */
    MessageCreatedEvent.MentionSnapshot toMentionSnapshot(MessageMention mention);

    /**
     * 批量将消息提及实体转换为事件快照.
     *
     * @param mentionList 消息提及实体列表
     * @return 消息提及事件快照列表
     */
    List<MessageCreatedEvent.MentionSnapshot> toMentionSnapshotList(List<MessageMention> mentionList);

    /**
     * 将消息资产信息转换为事件快照.
     *
     * @param assetInfo 消息资产信息
     * @return 消息资产事件快照
     */
    MessageCreatedEvent.AssetInfoSnapshot toAssetInfoSnapshot(MessageAssetInfoRespVO assetInfo);

    /**
     * 将会话用户信息转换为发送者事件快照.
     *
     * @param userInfo 会话用户信息
     * @return 发送者用户事件快照
     */
    MessageCreatedEvent.SenderUserSnapshot toSenderUserSnapshot(ConversationUserInfoRespVO userInfo);

    /**
     * 将AI消息扩展转换为发送者Agent事件快照.
     *
     * @param extension AI消息扩展
     * @return 发送者Agent事件快照
     */
    @Mapping(target = "displayName", constant = MessageAgentConstant.DEFAULT_DISPLAY_NAME)
    MessageCreatedEvent.SenderAgentSnapshot toSenderAgentSnapshot(MessageAgentExtension extension);

    /**
     * 将Agent引用转换为事件快照.
     *
     * @param citation Agent引用
     * @return Agent引用事件快照
     */
    MessageCreatedEvent.AgentCitationSnapshot toCitationSnapshot(AgentCitation citation);

    /**
     * 批量将Agent引用转换为事件快照.
     *
     * @param citationList Agent引用列表
     * @return Agent引用事件快照列表
     */
    List<MessageCreatedEvent.AgentCitationSnapshot> toCitationSnapshotList(List<AgentCitation> citationList);
}
