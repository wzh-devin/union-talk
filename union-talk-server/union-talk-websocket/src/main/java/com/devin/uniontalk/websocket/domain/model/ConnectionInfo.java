package com.devin.uniontalk.websocket.domain.model;

import com.devin.uniontalk.infrastructure.user.enums.DeviceTypeEnum;
import java.math.BigInteger;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * 可序列化连接信息
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
public class ConnectionInfo {

    /**
     * WebSocket 连接id.
     */
    private String connectionId;

    /**
     * 登录用户id.
     */
    private BigInteger userId;

    /**
     * 客户端设备id.
     */
    private String deviceId;

    /**
     * 客户端设备类型.
     */
    private DeviceTypeEnum deviceType;

    /**
     * 客户端 IP.
     */
    private String ip;

    /**
     * 连接所在服务节点id.
     */
    private String serverId;

    /**
     * 连接创建时间.
     */
    private Date connectedAt;

    /**
     * 最近心跳时间.
     */
    private Date lastHeartbeatAt;
}
