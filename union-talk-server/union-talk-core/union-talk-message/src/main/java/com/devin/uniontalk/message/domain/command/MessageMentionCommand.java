package com.devin.uniontalk.message.domain.command;

import com.devin.uniontalk.infrastructure.message.enums.MessageMentionTypeEnum;
import java.math.BigInteger;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/08/06 23:05.
 *
 * <p>
 * 消息提及输入命令
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageMentionCommand {

    /**
     * 提及类型.
     */
    private MessageMentionTypeEnum mentionType;

    /**
     * 提及目标id.
     */
    private BigInteger targetId;

    /**
     * 正文展示文本.
     */
    private String displayText;

    /**
     * 正文起始位置.
     */
    private Integer startOffset;

    /**
     * 正文文本长度.
     */
    private Integer length;

    /**
     * 判断提及区间与正文是否一致.
     *
     * @param content 消息正文
     * @return 是否一致
     */
    public boolean matchesContent(final String content) {
        if (Objects.isNull(content)
                || Objects.isNull(displayText)
                || Objects.isNull(startOffset)
                || Objects.isNull(length)
                || startOffset < 0
                || length <= 0
                || startOffset > content.length() - length) {
            return false;
        }
        return displayText.equals(content.substring(startOffset, startOffset + length));
    }

    /**
     * 获取提及区间结束位置.
     *
     * @return 区间结束位置
     */
    public int getEndOffset() {
        return startOffset + length;
    }
}
