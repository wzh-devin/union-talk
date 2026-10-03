package com.devin.uniontalk.websocket.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.devin.uniontalk.web.response.ResultEnum;
import com.devin.uniontalk.websocket.domain.enums.WsReqFrameTypeEnum;
import com.devin.uniontalk.websocket.domain.vo.req.WsBaseReqVO;
import com.devin.uniontalk.websocket.domain.vo.req.WsPingReqVO;
import com.devin.uniontalk.websocket.service.WebSocketConnectionService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Objects;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * Netty WebSocket 业务处理器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class NettyWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    /**
     * WebSocket 连接服务.
     */
    private final WebSocketConnectionService webSocketConnectionService;

    /**
     * 处理 WebSocket 事件.
     *
     * @param ctx Channel上下文
     * @param evt 事件
     * @throws Exception 异常
     */
    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleStateEvent) {
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                webSocketConnectionService.sendErrorAndClose(
                        ctx.channel(),
                        null,
                        ResultEnum.UNAUTHORIZED.getCode(),
                        "心跳超时"
                );
            }
            return;
        }
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            webSocketConnectionService.authorize(ctx.channel());
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    /**
     * Channel 被移除时清理连接.
     *
     * @param ctx Channel上下文
     */
    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) {
        webSocketConnectionService.remove(ctx.channel());
    }

    /**
     * Channel 不活跃时清理连接.
     *
     * @param ctx Channel上下文
     */
    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        webSocketConnectionService.remove(ctx.channel());
    }

    /**
     * 处理文本帧.
     *
     * @param ctx Channel上下文
     * @param msg 文本帧
     */
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final TextWebSocketFrame msg) {
        WsBaseReqVO req = readReq(ctx, msg.text(), WsBaseReqVO.class, null, "非法JSON帧");
        if (Objects.isNull(req)) {
            return;
        }
        WsReqFrameTypeEnum frameType = req.getType();
        if (Objects.isNull(frameType)) {
            webSocketConnectionService.sendError(
                    ctx.channel(),
                    req.getRequestId(),
                    ResultEnum.PARAM_ERROR.getCode(),
                    "WebSocket消息类型不能为空"
            );
            return;
        }
        switch (frameType) {
            case PING -> handlePing(ctx, msg.text(), req.getRequestId());
            default -> {
                log.error("未知WebSocket请求类型: {}", frameType);
                webSocketConnectionService.sendError(
                        ctx.channel(),
                        req.getRequestId(),
                        ResultEnum.PARAM_ERROR.getCode(),
                        "不支持的WebSocket消息类型"
                );
            }
        }
    }

    /**
     * 处理心跳请求帧.
     *
     * @param ctx       Channel上下文
     * @param text      原始文本帧
     * @param requestId 请求id
     */
    private void handlePing(final ChannelHandlerContext ctx, final String text, final String requestId) {
        WsPingReqVO req = readReq(ctx, text, WsPingReqVO.class, requestId, "非法PING请求帧");
        if (req == null) {
            return;
        }
        webSocketConnectionService.handleHeartbeat(ctx.channel(), req.getRequestId());
    }

    /**
     * 读取指定类型请求帧.
     *
     * @param ctx          Channel上下文
     * @param text         原始文本帧
     * @param reqClass     请求帧类型
     * @param requestId    请求id
     * @param errorMessage 错误消息
     * @param <T>          请求帧类型
     * @return 请求帧
     */
    private <T extends WsBaseReqVO> T readReq(
            final ChannelHandlerContext ctx,
            final String text,
            final Class<T> reqClass,
            final String requestId,
            final String errorMessage
    ) {
        try {
            return JSON.parseObject(text, reqClass);
        } catch (JSONException e) {
            log.warn("{}: {}", errorMessage, e.getMessage());
            webSocketConnectionService.sendError(ctx.channel(), requestId, ResultEnum.PARAM_ERROR.getCode(), errorMessage);
            return null;
        }
    }

    /**
     * 异常时关闭连接.
     *
     * @param ctx   Channel上下文
     * @param cause 异常
     */
    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        log.warn("WebSocket channel exception: {}", cause.getMessage(), cause);
        webSocketConnectionService.remove(ctx.channel());
        ctx.close();
    }
}
