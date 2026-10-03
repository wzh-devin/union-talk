package com.devin.uniontalk.message.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.message.domain.command.MessageMentionCommand;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import lombok.Data;

/**
 * 2026/08/06 23:05.
 *
 * <p>
 * 消息提及实体
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName("ut_message_mention")
public class MessageMention implements Serializable {

    /**
     * 序列化版本号.
     */
    @Serial
    private static final long serialVersionUID = 8033587033020805488L;

    /**
     * 提及记录id.
     */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /**
     * 消息id.
     */
    @TableField("message_id")
    private BigInteger messageId;

    /**
     * 会话id.
     */
    @TableField("conversation_id")
    private BigInteger conversationId;

    /**
     * 提及类型.
     */
    @TableField("mention_type")
    private String mentionType;

    /**
     * 提及目标id.
     */
    @TableField("target_id")
    private BigInteger targetId;

    /**
     * 正文展示文本.
     */
    @TableField("display_text")
    private String displayText;

    /**
     * 正文起始位置.
     */
    @TableField("start_offset")
    private Integer startOffset;

    /**
     * 正文文本长度.
     */
    @TableField("length")
    private Integer length;

    /**
     * 创建时间.
     */
    @TableField("created_at")
    private Date createdAt;

    /**
     * 根据消息与输入命令初始化提及记录.
     *
     * @param message 消息实体
     * @param command 提及输入命令
     */
    public void init(final Message message, final MessageMentionCommand command) {
        this.id = IdGenerator.nextIdBigInteger();
        this.messageId = message.getId();
        this.conversationId = message.getConversationId();
        this.mentionType = command.getMentionType().name();
        this.targetId = command.getTargetId();
        this.displayText = command.getDisplayText();
        this.startOffset = command.getStartOffset();
        this.length = command.getLength();
        this.createdAt = new Date();
    }

    /**
     * 从消息正文中移除当前提及展示文本.
     *
     * @param content 消息正文
     * @return 移除提及后的正文，区间无效时返回原正文
     */
    public String removeFromContent(final String content) {
        if (content == null
                || displayText == null
                || startOffset == null
                || length == null
                || startOffset < 0
                || length <= 0
                || startOffset > content.length() - length
                || !displayText.equals(content.substring(startOffset, startOffset + length))) {
            return content;
        }
        return content.substring(0, startOffset) + content.substring(startOffset + length);
    }
}
