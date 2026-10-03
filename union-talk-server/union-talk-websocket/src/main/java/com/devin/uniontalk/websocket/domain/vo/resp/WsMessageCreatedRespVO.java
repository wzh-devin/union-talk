package com.devin.uniontalk.websocket.domain.vo.resp;

import com.devin.uniontalk.infrastructure.file.enums.AssetFileTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.AgentCitationSourceTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageMentionTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageSenderTypeEnum;
import com.devin.uniontalk.infrastructure.message.enums.MessageTypeEnum;
import com.devin.uniontalk.infrastructure.user.enums.UserStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 2026/05/20 18:00.
 *
 * <p>
 * WebSocket 消息创建响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "WebSocket消息创建响应参数")
public class WsMessageCreatedRespVO extends WsBaseRespVO<WsMessageCreatedRespVO.WsMessageCreatedRespData> {

    /**
     * WebSocket 消息创建响应数据.
     */
    @Data
    @Schema(description = "WebSocket消息创建响应数据")
    public static class WsMessageCreatedRespData {

        /**
         * 消息id.
         */
        @Schema(description = "消息id")
        private BigInteger messageId;

        /**
         * 会话id.
         */
        @Schema(description = "会话id")
        private BigInteger conversationId;

        /**
         * 消息发送者类型.
         */
        @Schema(description = "消息发送者类型")
        private MessageSenderTypeEnum senderType;

        /**
         * 发送者用户信息.
         */
        @Schema(description = "发送者用户信息")
        private SenderUserData senderUser;

        /**
         * 发送者Agent信息.
         */
        @Schema(description = "发送者Agent信息")
        private SenderAgentData senderAgent;

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
        private AssetInfoData assetInfo;

        /**
         * 引用消息id.
         */
        @Schema(description = "引用消息id")
        private BigInteger quoteMsgId;

        /**
         * 结构化提及列表.
         */
        @Schema(description = "结构化提及列表")
        private List<MentionData> mentionList;

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
        private List<AgentCitationData> citationList;

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

    /**
     * 发送者用户信息.
     */
    @Data
    @Schema(description = "发送者用户信息")
    public static class SenderUserData {

        /**
         * 用户id.
         */
        @Schema(description = "用户id")
        private BigInteger userId;

        /**
         * 用户code.
         */
        @Schema(description = "用户code")
        private String code;

        /**
         * 用户名.
         */
        @Schema(description = "用户名")
        private String username;

        /**
         * 头像地址.
         */
        @Schema(description = "头像地址")
        private String avatarUrl;

        /**
         * 用户状态.
         */
        @Schema(description = "用户状态")
        private UserStatusEnum status;
    }

    /**
     * 发送者Agent信息.
     */
    @Data
    @Schema(description = "发送者Agent信息")
    public static class SenderAgentData {

        /**
         * 稳定Agent定义id.
         */
        @Schema(description = "稳定Agent定义id")
        private BigInteger agentId;

        /**
         * Agent显示名称.
         */
        @Schema(description = "Agent显示名称")
        private String displayName;
    }

    /**
     * 消息提及信息.
     */
    @Data
    @Schema(description = "消息提及信息")
    public static class MentionData {

        /**
         * 提及类型.
         */
        @Schema(description = "提及类型")
        private MessageMentionTypeEnum mentionType;

        /**
         * 提及目标id.
         */
        @Schema(description = "提及目标id")
        private BigInteger targetId;

        /**
         * 正文展示文本.
         */
        @Schema(description = "正文展示文本")
        private String displayText;

        /**
         * 正文起始位置.
         */
        @Schema(description = "正文起始位置")
        private Integer startOffset;

        /**
         * 正文文本长度.
         */
        @Schema(description = "正文文本长度")
        private Integer length;
    }

    /**
     * Agent引用信息.
     */
    @Data
    @Schema(description = "Agent引用信息")
    public static class AgentCitationData {

        /**
         * 引用标识.
         */
        @Schema(description = "引用标识")
        private String citationKey;

        /**
         * 来源类型.
         */
        @Schema(description = "来源类型")
        private AgentCitationSourceTypeEnum sourceType;

        /**
         * 消息id.
         */
        @Schema(description = "消息id")
        private BigInteger messageId;

        /**
         * 资产文件id.
         */
        @Schema(description = "资产文件id")
        private BigInteger assetFileId;

        /**
         * 资源版本.
         */
        @Schema(description = "资源版本")
        private Integer resourceVersion;

        /**
         * 资源块id.
         */
        @Schema(description = "资源块id")
        private BigInteger chunkId;

        /**
         * 起始页码.
         */
        @Schema(description = "起始页码")
        private Integer pageFrom;

        /**
         * 结束页码.
         */
        @Schema(description = "结束页码")
        private Integer pageTo;

        /**
         * 标题路径.
         */
        @Schema(description = "标题路径")
        private String headingPath;
    }

    /**
     * 消息资产文件信息.
     */
    @Data
    @Schema(description = "消息资产文件信息")
    public static class AssetInfoData {

        /**
         * 资产文件id.
         */
        @Schema(description = "资产文件id")
        private BigInteger id;

        /**
         * 会话id.
         */
        @Schema(description = "会话id")
        private BigInteger conversationId;

        /**
         * 文件名称.
         */
        @Schema(description = "文件名称")
        private String name;

        /**
         * 文件扩展名.
         */
        @Schema(description = "文件扩展名")
        private String fileExt;

        /**
         * 文件类型.
         */
        @Schema(description = "文件类型")
        private AssetFileTypeEnum fileType;

        /**
         * 文件大小.
         */
        @Schema(description = "文件大小")
        private BigInteger fileSize;

        /**
         * MIME类型.
         */
        @Schema(description = "MIME类型")
        private String mimeType;
    }
}
