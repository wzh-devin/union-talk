package com.devin.uniontalk.websocket.domain.vo.resp;

import com.devin.uniontalk.infrastructure.user.enums.DeviceTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * 在线连接响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "在线连接响应参数")
public class OnlineConnectionRespVO {

    /**
     * WebSocket 连接id.
     */
    @Schema(description = "连接id")
    private String connectionId;

    /**
     * 客户端设备id.
     */
    @Schema(description = "设备id")
    private String deviceId;

    /**
     * 客户端设备类型.
     */
    @Schema(description = "设备类型")
    private DeviceTypeEnum deviceType;

    /**
     * 客户端 IP.
     */
    @Schema(description = "客户端IP")
    private String ip;

    /**
     * 连接所在服务节点id.
     */
    @Schema(description = "连接所在服务节点")
    private String serverId;

    /**
     * 连接创建时间.
     */
    @Schema(description = "连接时间")
    private Date connectedAt;

    /**
     * 最近心跳时间.
     */
    @Schema(description = "最近心跳时间")
    private Date lastHeartbeatAt;
}
