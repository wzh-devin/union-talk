package com.devin.uniontalk.websocket.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * WebSocket 心跳请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "WebSocket心跳请求参数")
public class WsPingReqVO extends WsBaseReqVO {

    /**
     * 心跳请求数据.
     */
    @Schema(description = "心跳请求数据")
    private WsPingReqData data;

    /**
     * WebSocket 心跳请求数据.
     */
    @Data
    @Schema(description = "WebSocket心跳请求数据")
    public static class WsPingReqData {

        /**
         * 客户端发送心跳的时间戳.
         */
        @Schema(description = "客户端发送心跳的时间戳")
        private Long clientTime;
    }
}
