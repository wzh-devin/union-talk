package com.devin.uniontalk.websocket.domain.model;

import com.devin.uniontalk.infrastructure.user.enums.DeviceTypeEnum;
import io.netty.channel.Channel;
import java.math.BigInteger;
import java.util.Date;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * 本机连接上下文
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
public class ConnectionContext {

    /**
     * WebSocket 连接id.
     */
    private String connectionId;

    /**
     * 登录用户id.
     */
    private BigInteger userId;

    /**
     * 登录 token.
     */
    private String token;

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

    /**
     * Netty Channel.
     */
    private Channel channel;

    /**
     * 转换为可序列化连接信息.
     *
     * @return 连接信息
     */
    public ConnectionInfo toInfo() {
        return ConnectionInfo.builder()
                .connectionId(connectionId)
                .userId(userId)
                .deviceId(deviceId)
                .deviceType(deviceType)
                .ip(ip)
                .serverId(serverId)
                .connectedAt(connectedAt)
                .lastHeartbeatAt(lastHeartbeatAt)
                .build();
    }
}
