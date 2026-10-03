package com.devin.uniontalk.rabbitmq.domain.event;

import com.devin.uniontalk.infrastructure.file.enums.AssetFileTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.AgentCitationSourceTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageMentionTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageOutboxEventTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageSenderTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import com.devin.uniontalk.infrastructure.user.enums.UserStatusEnum;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/05/20 18:00.
 *
 * <p>
 * 消息创建事件
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
public class MessageCreatedEvent {

    /**
     * 事件id.
     */
    private String eventId;

    /**
     * 事件类型.
     */
    private MessageOutboxEventTypeEnum eventType;

    /**
     * 事件Schema版本.
     */
    private Integer schemaVersion;

    /**
     * 消息id.
     */
    private BigInteger messageId;

    /**
     * 会话id.
     */
    private BigInteger conversationId;

    /**
     * 发送者id.
     */
    private BigInteger senderId;

    /**
     * 消息发送者类型.
     */
    private MessageSenderTypeEnum senderType;

    /**
     * 发送者用户快照.
     */
    private SenderUserSnapshot senderUser;

    /**
     * 发送者Agent快照.
     */
    private SenderAgentSnapshot senderAgent;

    /**
     * 消息类型.
     */
    private MessageTypeEnum type;

    /**
     * 消息内容.
     */
    private String content;

    /**
     * 消息资产文件快照.
     */
    private AssetInfoSnapshot assetInfo;

    /**
     * 引用消息id.
     */
    private BigInteger quoteMsgId;

    /**
     * 结构化提及快照列表.
     */
    private List<MentionSnapshot> mentionList;

    /**
     * Agent运行id.
     */
    private BigInteger agentRunId;

    /**
     * 触发消息id.
     */
    private BigInteger triggerMessageId;

    /**
     * Agent引用快照列表.
     */
    private List<AgentCitationSnapshot> citationList;

    /**
     * 是否撤回.
     */
    private Boolean recalled;

    /**
     * 需要推送的用户id列表.
     */
    private List<BigInteger> receiverUserIdList;

    /**
     * 创建时间.
     */
    private Date createdAt;

    /**
     * 发送者用户快照.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderUserSnapshot {

        /**
         * 用户id.
         */
        private BigInteger userId;

        /**
         * 用户code.
         */
        private String code;

        /**
         * 用户名.
         */
        private String username;

        /**
         * 头像地址.
         */
        private String avatarUrl;

        /**
         * 用户状态.
         */
        private UserStatusEnum status;
    }

    /**
     * 发送者Agent快照.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderAgentSnapshot {

        /**
         * 稳定Agent定义id.
         */
        private BigInteger agentId;

        /**
         * Agent显示名称.
         */
        private String displayName;
    }

    /**
     * 消息提及快照.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MentionSnapshot {

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
    }

    /**
     * Agent引用快照.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentCitationSnapshot {

        /**
         * 引用标识.
         */
        private String citationKey;

        /**
         * 来源类型.
         */
        private AgentCitationSourceTypeEnum sourceType;

        /**
         * 消息id.
         */
        private BigInteger messageId;

        /**
         * 资产文件id.
         */
        private BigInteger assetFileId;

        /**
         * 资源版本.
         */
        private Integer resourceVersion;

        /**
         * 资源块id.
         */
        private BigInteger chunkId;

        /**
         * 起始页码.
         */
        private Integer pageFrom;

        /**
         * 结束页码.
         */
        private Integer pageTo;

        /**
         * 标题路径.
         */
        private String headingPath;
    }

    /**
     * 消息资产文件快照.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetInfoSnapshot {

        /**
         * 资产文件id.
         */
        private BigInteger id;

        /**
         * 会话id.
         */
        private BigInteger conversationId;

        /**
         * 文件名称.
         */
        private String name;

        /**
         * 文件扩展名.
         */
        private String fileExt;

        /**
         * 文件类型.
         */
        private AssetFileTypeEnum fileType;

        /**
         * 文件大小.
         */
        private BigInteger fileSize;

        /**
         * MIME类型.
         */
        private String mimeType;
    }
}
