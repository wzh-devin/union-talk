package com.devin.uniontalk.message.domain.entity.convertor;

import com.devin.uniontalk.infrastructure.message.constant.MessageAgentConstant;
import com.devin.uniontalk.message.domain.command.MessageMentionCommand;
import com.devin.uniontalk.message.domain.entity.Message;
import com.devin.uniontalk.message.domain.entity.MessageAgentExtension;
import com.devin.uniontalk.message.domain.entity.MessageMention;
import com.devin.uniontalk.message.domain.model.AgentCitation;
import com.devin.uniontalk.message.domain.vo.req.MessageMentionReqVO;
import com.devin.uniontalk.message.domain.vo.resp.AgentCitationRespVO;
import com.devin.uniontalk.message.domain.vo.resp.MessageAgentInfoRespVO;
import com.devin.uniontalk.message.domain.vo.resp.MessageMentionRespVO;
import com.devin.uniontalk.message.domain.vo.resp.MessageRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 2026/05/20 16:30.
 *
 * <p>
 * 消息领域数据转换器
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
public interface MessageConvertor {

    /**
     * 消息转换器实例.
     */
    MessageConvertor INSTANCE = Mappers.getMapper(MessageConvertor.class);

    /**
     * 将消息实体转换为响应VO.
     *
     * @param message 消息实体
     * @return 消息响应VO
     */
    MessageRespVO toRespVO(Message message);

    /**
     * 批量将消息实体转换为响应VO.
     *
     * @param messageList 消息实体列表
     * @return 消息响应VO列表
     */
    List<MessageRespVO> toRespVOList(List<Message> messageList);

    /**
     * 将消息提及请求参数转换为输入命令.
     *
     * @param reqVO 消息提及请求参数
     * @return 消息提及输入命令
     */
    MessageMentionCommand toMentionCommand(MessageMentionReqVO reqVO);

    /**
     * 批量将消息提及请求参数转换为输入命令.
     *
     * @param reqVOList 消息提及请求参数列表
     * @return 消息提及输入命令列表
     */
    List<MessageMentionCommand> toMentionCommandList(List<MessageMentionReqVO> reqVOList);

    /**
     * 将消息提及实体转换为响应参数.
     *
     * @param mention 消息提及实体
     * @return 消息提及响应参数
     */
    MessageMentionRespVO toMentionRespVO(MessageMention mention);

    /**
     * 批量将消息提及实体转换为响应参数.
     *
     * @param mentionList 消息提及实体列表
     * @return 消息提及响应参数列表
     */
    List<MessageMentionRespVO> toMentionRespVOList(List<MessageMention> mentionList);

    /**
     * 将AI消息扩展转换为Agent响应信息.
     *
     * @param extension AI消息扩展
     * @return Agent响应信息
     */
    @Mapping(target = "displayName", constant = MessageAgentConstant.DEFAULT_DISPLAY_NAME)
    MessageAgentInfoRespVO toAgentInfoRespVO(MessageAgentExtension extension);

    /**
     * 将Agent引用转换为响应参数.
     *
     * @param citation Agent引用
     * @return Agent引用响应参数
     */
    AgentCitationRespVO toCitationRespVO(AgentCitation citation);

    /**
     * 批量将Agent引用转换为响应参数.
     *
     * @param citationList Agent引用列表
     * @return Agent引用响应参数列表
     */
    List<AgentCitationRespVO> toCitationRespVOList(List<AgentCitation> citationList);

}
