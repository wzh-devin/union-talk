package com.devin.uniontalk.netty.properties;

import io.netty.util.NettyRuntime;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * Netty WebSocket 配置属性
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "union.netty.websocket")
public class NettyWebSocketProperties {

    /**
     * WebSocket 监听端口.
     */
    private Integer port = 14001;

    /**
     * WebSocket 握手路径.
     */
    private String path = "/api/v1/ws";

    /**
     * Boss 线程数.
     */
    private Integer bossThreads = 1;

    /**
     * Worker 线程数，配置小于等于0时使用CPU核心数.
     */
    private Integer workerThreads = 0;

    /**
     * 读空闲超时时间，单位秒.
     */
    private Integer readerIdleSeconds = 30;

    /**
     * HTTP 请求聚合最大长度.
     */
    private Integer maxContentLength = 8192;

    /**
     * WebSocket 帧最大载荷长度.
     */
    private Integer maxFramePayloadLength = 65536;

    /**
     * TCP backlog.
     */
    private Integer soBacklog = 128;

    /**
     * 是否开启 TCP keepalive.
     */
    private Boolean soKeepAlive = Boolean.TRUE;

    /**
     * 连接在线状态缓存过期时间，单位秒.
     */
    private Integer connectionTtlSeconds = 90;

    /**
     * 当前 Netty 节点标识，为空时由应用自动生成.
     */
    private String serverId = "";

    /**
     * 获取实际 Worker 线程数.
     *
     * @return Worker 线程数
     */
    public int resolveWorkerThreads() {
        if (workerThreads == null || workerThreads <= 0) {
            return NettyRuntime.availableProcessors();
        }
        return workerThreads;
    }
}
