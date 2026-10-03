package com.devin.uniontalk.websocket.domain.vo.resp;

import com.devin.uniontalk.websocket.domain.enums.WsRespFrameTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * WebSocket 响应帧基础参数
 * </p>
 *
 * @param <T> 数据类型
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "WebSocket响应帧基础参数")
public class WsBaseRespVO<T> {

    /**
     * WebSocket 响应帧类型.
     */
    @Schema(description = "WebSocket响应帧类型")
    private WsRespFrameTypeEnum type;

    /**
     * 客户端请求id.
     */
    @Schema(description = "客户端请求id")
    private String requestId;

    /**
     * 响应是否成功.
     */
    @Schema(description = "响应是否成功")
    private Boolean success;

    /**
     * 响应状态码.
     */
    @Schema(description = "响应状态码")
    private Integer code;

    /**
     * 响应消息.
     */
    @Schema(description = "响应消息")
    private String message;

    /**
     * 响应生成时间戳.
     */
    @Schema(description = "响应生成时间戳")
    private Long timestamp;

    /**
     * WebSocket 业务数据.
     */
    @Schema(description = "WebSocket业务数据")
    private T data;
}
