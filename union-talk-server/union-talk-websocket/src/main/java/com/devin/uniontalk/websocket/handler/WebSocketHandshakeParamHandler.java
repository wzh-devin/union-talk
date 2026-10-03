package com.devin.uniontalk.websocket.handler;

import com.devin.uniontalk.netty.utils.NettyAttrKeys;
import com.devin.uniontalk.netty.utils.NettyChannelUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.net.InetSocketAddress;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * WebSocket 握手参数处理器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public class WebSocketHandshakeParamHandler extends ChannelInboundHandlerAdapter {

    /**
     * token 查询参数名.
     */
    private static final String TOKEN_QUERY_KEY = "token";

    /**
     * 设备id查询参数名.
     */
    private static final String DEVICE_ID_QUERY_KEY = "deviceId";

    /**
     * 设备类型查询参数名.
     */
    private static final String DEVICE_TYPE_QUERY_KEY = "deviceType";

    /**
     * Authorization 请求头 Bearer 前缀.
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 读取握手请求中的 token、设备信息和客户端 IP.
     *
     * @param ctx Channel上下文
     * @param msg 入站消息
     * @throws Exception 异常
     */
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof FullHttpRequest request) {
            QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
            NettyChannelUtils.setAttr(ctx.channel(), NettyAttrKeys.TOKEN, resolveToken(decoder, request.headers()));
            NettyChannelUtils.setAttr(ctx.channel(), NettyAttrKeys.DEVICE_ID, firstQueryParam(decoder, DEVICE_ID_QUERY_KEY));
            NettyChannelUtils.setAttr(ctx.channel(), NettyAttrKeys.DEVICE_TYPE, firstQueryParam(decoder, DEVICE_TYPE_QUERY_KEY));
            NettyChannelUtils.setAttr(ctx.channel(), NettyAttrKeys.IP, resolveIp(ctx, request.headers()));
            request.setUri(decoder.path());
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(request);
            return;
        }
        ctx.fireChannelRead(msg);
    }

    /**
     * 解析 token，优先 query 参数，其次 Authorization 请求头.
     *
     * @param decoder HTTP URI 解码器
     * @param headers HTTP 请求头
     * @return token
     */
    private String resolveToken(final QueryStringDecoder decoder, final HttpHeaders headers) {
        String token = firstQueryParam(decoder, TOKEN_QUERY_KEY);
        if (StringUtils.hasText(token)) {
            return token;
        }
        String authorization = headers.get(HttpHeaderNames.AUTHORIZATION);
        if (!StringUtils.hasText(authorization)) {
            return "";
        }
        if (authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length()).trim();
        }
        return authorization.trim();
    }

    /**
     * 获取第一个 query 参数.
     *
     * @param decoder HTTP URI 解码器
     * @param key     参数名
     * @return 参数值
     */
    private String firstQueryParam(final QueryStringDecoder decoder, final String key) {
        List<String> values = decoder.parameters().get(key);
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.getFirst();
    }

    /**
     * 解析客户端 IP.
     *
     * @param ctx     Channel上下文
     * @param headers HTTP 请求头
     * @return 客户端 IP
     */
    private String resolveIp(final ChannelHandlerContext ctx, final HttpHeaders headers) {
        String realIp = headers.get("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        String forwardedFor = headers.get("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        if (ctx.channel().remoteAddress() instanceof InetSocketAddress address) {
            return address.getAddress().getHostAddress();
        }
        return "";
    }
}
