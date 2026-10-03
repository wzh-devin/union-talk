package com.devin.uniontalk.message.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.infrastructure.message.enums.MessageSenderTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import lombok.Data;

/**
 * 2026/05/20 15:20:24.
 *
 * <p>
 *  消息表(Message)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_message", autoResultMap = true)
public class Message implements Serializable {

    /**
     * 序列化版本号.
     */
    @Serial
    private static final long serialVersionUID = 318286810808326206L;

    /**
     * 主键id.
     */
    @TableId
    private BigInteger id;

    /**
     * 会话id.
     */
    @TableField("conversation_id")
    private BigInteger conversationId;

    /**
     * 发送者id.
     */
    @TableField("sender_id")
    private BigInteger senderId;

    /**
     * 消息发送者类型.
     */
    @TableField("sender_type")
    private String senderType;

    /**
     * 消息类型.
     */
    @TableField("type")
    private String type;

    /**
     * 消息内容.
     */
    @TableField("content")
    private String content;

    /**
     * 引用的消息id.
     */
    @TableField("quote_msg_id")
    private BigInteger quoteMsgId;

    /**
     * 是否撤回.
     */
    @TableField("recalled")
    private Boolean recalled;

    /**
     * 撤回时间.
     */
    @TableField("recalled_at")
    private Date recalledAt;

    /**
     * 删除时间.
     */
    @TableField("deleted_at")
    private Date deletedAt;

    /**
     * 创建时间.
     */
    @TableField("created_at")
    private Date createdAt;

    /**
     * 更新时间.
     */
    @TableField("updated_at")
    private Date updatedAt;

    /**
     * 初始化用户消息.
     *
     * @param senderId       发送者id
     * @param conversationId 会话id
     * @param type           消息类型
     * @param content        消息内容
     * @param quoteMsgId     引用消息id
     */
    public void initUserMessage(
            final BigInteger senderId,
            final BigInteger conversationId,
            final MessageTypeEnum type,
            final String content,
            final BigInteger quoteMsgId
    ) {
        this.id = IdGenerator.nextIdBigInteger();
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderType = MessageSenderTypeEnum.USER.name();
        this.type = type.name();
        this.content = content;
        this.quoteMsgId = quoteMsgId;
        this.recalled = Boolean.FALSE;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    /**
     * 初始化Agent回复消息.
     *
     * @param conversationId  会话id
     * @param content         回复内容
     * @param triggerMessageId 触发消息id
     */
    public void initAgentMessage(
            final BigInteger conversationId,
            final String content,
            final BigInteger triggerMessageId
    ) {
        this.id = IdGenerator.nextIdBigInteger();
        this.conversationId = conversationId;
        this.senderId = null;
        this.senderType = MessageSenderTypeEnum.AGENT.name();
        this.type = MessageTypeEnum.TEXT.name();
        this.content = content;
        this.quoteMsgId = triggerMessageId;
        this.recalled = Boolean.FALSE;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
}
