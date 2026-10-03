package com.devin.uniontalk.websocket.domain.vo.resp;

import com.devin.uniontalk.infrastructure.user.enums.FriendRequestStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 2026/05/20 18:30.
 *
 * <p>
 * WebSocket 好友申请同意响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "WebSocket好友申请同意响应参数")
public class WsFriendRequestAcceptedRespVO extends WsBaseRespVO<WsFriendRequestAcceptedRespVO.WsFriendRequestAcceptedRespData> {

    /**
     * WebSocket 好友申请同意响应数据.
     */
    @Data
    @Schema(description = "WebSocket好友申请同意响应数据")
    public static class WsFriendRequestAcceptedRespData {

        /**
         * 好友申请id.
         */
        @Schema(description = "好友申请id")
        private BigInteger friendRequestId;

        /**
         * 申请用户id.
         */
        @Schema(description = "申请用户id")
        private BigInteger fromUserId;

        /**
         * 同意用户id.
         */
        @Schema(description = "同意用户id")
        private BigInteger toUserId;

        /**
         * 同意用户名.
         */
        @Schema(description = "同意用户名")
        private String toUsername;

        /**
         * 同意用户头像.
         */
        @Schema(description = "同意用户头像")
        private String toAvatarUrl;

        /**
         * 会话id.
         */
        @Schema(description = "会话id")
        private BigInteger conversationId;

        /**
         * 申请状态.
         */
        @Schema(description = "申请状态")
        private FriendRequestStatusEnum status;

        /**
         * 处理时间.
         */
        @Schema(description = "处理时间")
        private Date handledAt;

        /**
         * 推送内容.
         */
        @Schema(description = "推送内容")
        private String content;
    }
}
