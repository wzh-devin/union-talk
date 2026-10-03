package com.devin.uniontalk.websocket.domain.vo.req;

import com.devin.uniontalk.websocket.domain.enums.WsReqFrameTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * WebSocket 请求帧基础参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "WebSocket请求帧基础参数")
public class WsBaseReqVO {

    /**
     * WebSocket 请求帧类型.
     */
    @Schema(description = "WebSocket请求帧类型")
    private WsReqFrameTypeEnum type;

    /**
     * 客户端请求id.
     */
    @Schema(description = "客户端请求id")
    private String requestId;
}
