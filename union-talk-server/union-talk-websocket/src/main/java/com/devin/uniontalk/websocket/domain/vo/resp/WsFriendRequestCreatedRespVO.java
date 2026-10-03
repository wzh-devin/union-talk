package com.devin.uniontalk.websocket.domain.vo.resp;

import com.devin.uniontalk.infrastructure.user.enums.FriendRequestStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 2026/05/20 00:00.
 *
 * <p>
 * WebSocket 好友申请创建响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "WebSocket好友申请创建响应参数")
public class WsFriendRequestCreatedRespVO extends WsBaseRespVO<WsFriendRequestCreatedRespVO.WsFriendRequestCreatedRespData> {

    /**
     * WebSocket 好友申请创建响应数据.
     */
    @Data
    @Schema(description = "WebSocket好友申请创建响应数据")
    public static class WsFriendRequestCreatedRespData {

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
         * 目标用户id.
         */
        @Schema(description = "目标用户id")
        private BigInteger toUserId;

        /**
         * 申请用户名.
         */
        @Schema(description = "申请用户名")
        private String fromUsername;

        /**
         * 申请用户头像.
         */
        @Schema(description = "申请用户头像")
        private String fromAvatarUrl;

        /**
         * 申请信息.
         */
        @Schema(description = "申请信息")
        private String applyMsg;

        /**
         * 申请状态.
         */
        @Schema(description = "申请状态")
        private FriendRequestStatusEnum status;

        /**
         * 创建时间.
         */
        @Schema(description = "创建时间")
        private Date createdAt;
    }
}
