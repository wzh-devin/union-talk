package com.devin.uniontalk.message.domain.vo.resp;

import com.devin.uniontalk.infrastructure.message.enums.MessageSenderTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/05/20 16:00.
 *
 * <p>
 * 消息响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "消息响应参数")
public class MessageRespVO {

    /**
     * 消息id.
     */
    @Schema(description = "消息id")
    private BigInteger id;

    /**
     * 会话id.
     */
    @Schema(description = "会话id")
    private BigInteger conversationId;

    /**
     * 发送者id.
     */
    @Schema(description = "发送者id")
    private BigInteger senderId;

    /**
     * 消息发送者类型.
     */
    @Schema(description = "消息发送者类型")
    private MessageSenderTypeEnum senderType;

    /**
     * 发送者用户信息.
     */
    @Schema(description = "发送者用户信息")
    private ConversationUserInfoRespVO senderUser;

    /**
     * 发送者Agent信息.
     */
    @Schema(description = "发送者Agent信息")
    private MessageAgentInfoRespVO senderAgent;

    /**
     * 消息类型.
     */
    @Schema(description = "消息类型")
    private MessageTypeEnum type;

    /**
     * 消息内容.
     */
    @Schema(description = "消息内容")
    private String content;

    /**
     * 消息资产文件信息.
     */
    @Schema(description = "消息资产文件信息")
    private MessageAssetInfoRespVO assetInfo;

    /**
     * 引用消息id.
     */
    @Schema(description = "引用消息id")
    private BigInteger quoteMsgId;

    /**
     * 结构化提及列表.
     */
    @Schema(description = "结构化提及列表")
    private List<MessageMentionRespVO> mentionList;

    /**
     * Agent运行id.
     */
    @Schema(description = "Agent运行id")
    private BigInteger agentRunId;

    /**
     * 触发消息id.
     */
    @Schema(description = "触发消息id")
    private BigInteger triggerMessageId;

    /**
     * Agent引用列表.
     */
    @Schema(description = "Agent引用列表")
    private List<AgentCitationRespVO> citationList;

    /**
     * 是否撤回.
     */
    @Schema(description = "是否撤回")
    private Boolean recalled;

    /**
     * 创建时间.
     */
    @Schema(description = "创建时间")
    private Date createdAt;
}
