package com.devin.uniontalk.websocket.config;

import com.devin.uniontalk.netty.properties.NettyWebSocketProperties;
import com.devin.uniontalk.websocket.handler.NettyWebSocketFrameHandler;
import com.devin.uniontalk.websocket.handler.WebSocketHandshakeParamHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * Netty WebSocket 服务启动器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyWebSocketServer {

    /**
     * Netty WebSocket 配置属性.
     */
    private final NettyWebSocketProperties properties;

    /**
     * WebSocket 业务帧处理器.
     */
    private final NettyWebSocketFrameHandler frameHandler;

    /**
     * Netty 主线程组.
     */
    private EventLoopGroup bossGroup;

    /**
     * Netty 工作线程组.
     */
    private EventLoopGroup workerGroup;

    /**
     * Netty 服务端 Channel.
     */
    private Channel serverChannel;

    /**
     * 启动 Netty WebSocket 服务.
     */
    //CHECKSTYLE:OFF
    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(properties.getBossThreads());
        workerGroup = new NioEventLoopGroup(properties.resolveWorkerThreads());
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, properties.getSoBacklog())
                    .childOption(ChannelOption.SO_KEEPALIVE, properties.getSoKeepAlive())
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(final SocketChannel socketChannel) {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            // 客户端超过读空闲时间未发送心跳时主动断开连接。
                            pipeline.addLast(
                                    new IdleStateHandler(
                                            properties.getReaderIdleSeconds(),
                                            0,
                                            0
                                    )
                            );
                            // WebSocket 初始握手基于 HTTP 协议，先完成 HTTP 编解码和聚合。
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new ChunkedWriteHandler());
                            pipeline.addLast(new HttpObjectAggregator(properties.getMaxContentLength()));
                            // 握手升级前解析 token、设备信息和客户端 IP。
                            pipeline.addLast(new WebSocketHandshakeParamHandler());
                            pipeline.addLast(
                                    new WebSocketServerProtocolHandler(
                                            properties.getPath(),
                                            null,
                                            true,
                                            properties.getMaxFramePayloadLength()
                                    )
                            );
                            pipeline.addLast(frameHandler);
                        }
                    });
            ChannelFuture channelFuture = serverBootstrap.bind(properties.getPort()).sync();
            serverChannel = channelFuture.channel();
            log.info("Netty WebSocket server started, port={}, path={}", properties.getPort(), properties.getPath());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Netty WebSocket 服务启动被中断", e);
        }
    }
    //CHECKSTYLE:ON

    /**
     * 关闭 Netty WebSocket 服务.
     */
    @PreDestroy
    public void destroy() {
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
        }
        shutdownGracefully(bossGroup);
        shutdownGracefully(workerGroup);
        log.info("Netty WebSocket server stopped");
    }

    /**
     * 优雅关闭线程组.
     *
     * @param eventLoopGroup 线程组
     */
    private void shutdownGracefully(final EventLoopGroup eventLoopGroup) {
        if (eventLoopGroup == null) {
            return;
        }
        Future<?> future = eventLoopGroup.shutdownGracefully();
        future.syncUninterruptibly();
    }
}
