package com.devin.uniontalk.websocket.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONWriter;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.cache.constant.CacheConstant;
import com.devin.uniontalk.cache.utils.RedisUtils;
import com.devin.uniontalk.infrastructure.user.enums.DeviceTypeEnum;
import com.devin.uniontalk.netty.properties.NettyWebSocketProperties;
import com.devin.uniontalk.netty.utils.NettyAttrKeys;
import com.devin.uniontalk.netty.utils.NettyChannelUtils;
import com.devin.uniontalk.web.response.ResultEnum;
import com.devin.uniontalk.websocket.domain.model.ConnectionContext;
import com.devin.uniontalk.websocket.domain.model.ConnectionInfo;
import com.devin.uniontalk.websocket.domain.vo.resp.OnlineConnectionRespVO;
import com.devin.uniontalk.websocket.domain.vo.resp.OnlineStatusRespVO;
import com.devin.uniontalk.websocket.domain.vo.resp.WsBaseRespVO;
import com.devin.uniontalk.websocket.service.WebSocketConnectionService;
import com.devin.uniontalk.websocket.service.adapter.WsFrameAdapter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * WebSocket 连接服务实现
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketConnectionServiceImpl implements WebSocketConnectionService {

    /**
     * 默认设备id.
     */
    private static final String DEFAULT_DEVICE_ID = "UNKNOWN";

    /**
     * Netty WebSocket 配置属性.
     */
    private final NettyWebSocketProperties properties;

    /**
     * Spring 环境变量.
     */
    private final Environment environment;

    /**
     * 本机 Channel 与连接上下文映射.
     */
    private final ConcurrentMap<Channel, ConnectionContext> channelContextMap = new ConcurrentHashMap<>();

    /**
     * 本机连接id与连接上下文映射.
     */
    private final ConcurrentMap<String, ConnectionContext> connectionContextMap = new ConcurrentHashMap<>();

    /**
     * 本机用户id与连接id集合映射.
     * 使用 ConcurrentHashMap.newKeySet() 代替 CopyOnWriteArraySet，
     * add/remove 均为 O(1)，避免写时复制的 O(n) 拷贝开销.
     */
    private final ConcurrentMap<BigInteger, Set<String>> userConnectionMap = new ConcurrentHashMap<>();

    /**
     * 当前 WebSocket 服务节点id.
     */
    private String serverId;

    /**
     * 初始化服务节点标识.
     */
    @PostConstruct
    public void init() {
        if (StringUtils.hasText(properties.getServerId())) {
            serverId = properties.getServerId();
            return;
        }
        String appName = environment.getProperty("spring.application.name", "union-talk-websocket");
        serverId = appName + "-" + properties.getPort() + "-" + IdGenerator.nextKey(8);
    }

    @Override
    public void authorize(final Channel channel) {
        ConnectionContext currentContext = channelContextMap.get(channel);
        if (Objects.nonNull(currentContext)) {
            refreshRedisState(currentContext);
            send(channel, WsFrameAdapter.connectAck(null, currentContext.toInfo()));
            return;
        }
        String token = NettyChannelUtils.getAttr(channel, NettyAttrKeys.TOKEN);
        if (!StringUtils.hasText(token)) {
            sendErrorAndClose(channel, null, ResultEnum.UNAUTHORIZED.getCode(), "token不能为空");
            return;
        }
        BigInteger userId = resolveUserId(token);
        if (Objects.isNull(userId)) {
            sendErrorAndClose(channel, null, ResultEnum.UNAUTHORIZED.getCode(), "token无效或已过期");
            return;
        }
        ConnectionContext context = buildConnectionContext(channel, token, userId);
        registerConnection(channel, context);
        refreshRedisState(context);
        send(channel, WsFrameAdapter.connectAck(null, context.toInfo()));
        log.info("WebSocket connected, userId={}, connectionId={}, deviceType={}",
                userId, context.getConnectionId(), context.getDeviceType());
    }

    @Override
    public void remove(final Channel channel) {
        ConnectionContext context = channelContextMap.remove(channel);
        if (Objects.isNull(context)) {
            return;
        }
        connectionContextMap.remove(context.getConnectionId());
        Set<String> connectionSet = userConnectionMap.get(context.getUserId());
        if (Objects.nonNull(connectionSet)) {
            connectionSet.remove(context.getConnectionId());
            if (connectionSet.isEmpty()) {
                userConnectionMap.remove(context.getUserId(), connectionSet);
            }
        }
        removeRedisState(context);
        log.info("WebSocket disconnected, userId={}, connectionId={}", context.getUserId(), context.getConnectionId());
    }

    @Override
    public void handleHeartbeat(final Channel channel, final String requestId) {
        ConnectionContext context = channelContextMap.get(channel);
        if (Objects.isNull(context)) {
            sendErrorAndClose(channel, requestId, ResultEnum.UNAUTHORIZED.getCode(), "连接未完成鉴权");
            return;
        }
        context.setLastHeartbeatAt(new Date());
        refreshRedisState(context);
        send(channel, WsFrameAdapter.pong(requestId));
    }

    @Override
    public void sendErrorAndClose(
            final Channel channel,
            final String requestId,
            final Integer code,
            final String message
    ) {
        write(channel, WsFrameAdapter.error(requestId, code, message), true);
    }

    @Override
    public void sendError(final Channel channel, final String requestId, final Integer code, final String message) {
        write(channel, WsFrameAdapter.error(requestId, code, message), false);
    }

    @Override
    public int sendToUser(final BigInteger userId, final WsBaseRespVO<?> resp) {
        Set<String> connectionIdSet = userConnectionMap.get(userId);
        if (connectionIdSet == null || connectionIdSet.isEmpty()) {
            return 0;
        }
        int sentConnectionCount = 0;
        for (String connectionId : connectionIdSet) {
            ConnectionContext context = connectionContextMap.get(connectionId);
            if (Objects.nonNull(context) && send(context.getChannel(), resp)) {
                sentConnectionCount++;
            }
        }
        return sentConnectionCount;
    }

    @Override
    public OnlineStatusRespVO getOnlineStatus(final BigInteger userId) {
        List<ConnectionInfo> connectionInfoList = listOnlineConnectionInfo(userId);
        List<OnlineConnectionRespVO> connectionRespList = connectionInfoList.stream()
                .map(this::toRespVO)
                .toList();
        return OnlineStatusRespVO.builder()
                .userId(userId)
                .online(!connectionRespList.isEmpty())
                .connectionCount(connectionRespList.size())
                .connectionList(connectionRespList)
                .build();
    }

    @Override
    public Map<BigInteger, Boolean> checkOnline(final Collection<BigInteger> userIdList) {
        Map<BigInteger, Boolean> result = new LinkedHashMap<>();
        for (BigInteger userId : userIdList) {
            result.put(userId, isLocalOnline(userId) || isRedisOnline(userId));
        }
        return result;
    }

    /**
     * 解析 token 对应的用户id.
     *
     * @param token token
     * @return 用户id
     */
    private BigInteger resolveUserId(final String token) {
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (Objects.isNull(loginId)) {
                return null;
            }
            return new BigInteger(loginId.toString());
        } catch (RuntimeException e) {
            log.warn("WebSocket token resolve failed", e);
            return null;
        }
    }

    /**
     * 构建连接上下文.
     *
     * @param channel Channel
     * @param token   token
     * @param userId  用户id
     * @return 连接上下文
     */
    private ConnectionContext buildConnectionContext(final Channel channel, final String token, final BigInteger userId) {
        Date now = new Date();
        return ConnectionContext.builder()
                .connectionId(IdGenerator.nextKey())
                .userId(userId)
                .token(token)
                .deviceId(defaultIfBlank(NettyChannelUtils.getAttr(channel, NettyAttrKeys.DEVICE_ID), DEFAULT_DEVICE_ID))
                .deviceType(DeviceTypeEnum.of(NettyChannelUtils.getAttr(channel, NettyAttrKeys.DEVICE_TYPE)))
                .ip(defaultIfBlank(NettyChannelUtils.getAttr(channel, NettyAttrKeys.IP), ""))
                .serverId(serverId)
                .connectedAt(now)
                .lastHeartbeatAt(now)
                .channel(channel)
                .build();
    }

    /**
     * 注册本机连接索引.
     *
     * @param channel Channel
     * @param context 连接上下文
     */
    private void registerConnection(final Channel channel, final ConnectionContext context) {
        NettyChannelUtils.setAttr(channel, NettyAttrKeys.USER_ID, context.getUserId());
        NettyChannelUtils.setAttr(channel, NettyAttrKeys.CONNECTION_ID, context.getConnectionId());
        channelContextMap.put(channel, context);
        connectionContextMap.put(context.getConnectionId(), context);
        userConnectionMap.computeIfAbsent(context.getUserId(), key -> ConcurrentHashMap.newKeySet())
                .add(context.getConnectionId());
    }

    /**
     * 查询用户所有在线连接信息（本机 + 跨节点 Redis，按 connectionId 去重）.
     *
     * @param userId 用户id
     * @return 在线连接信息列表
     */
    private List<ConnectionInfo> listOnlineConnectionInfo(final BigInteger userId) {
        Map<String, ConnectionInfo> merged = new LinkedHashMap<>();
        collectLocalConnections(userId, merged);
        collectRedisConnections(userId, merged);
        return new ArrayList<>(merged.values());
    }

    /**
     * 收集本机在线连接信息.
     *
     * @param userId 用户id
     * @param merged 合并结果容器，以 connectionId 为 key 保证去重
     */
    private void collectLocalConnections(final BigInteger userId, final Map<String, ConnectionInfo> merged) {
        Set<String> localIds = userConnectionMap.get(userId);
        if (localIds == null || localIds.isEmpty()) {
            return;
        }
        localIds.stream()
                .map(connectionContextMap::get)
                .filter(Objects::nonNull)
                .map(ConnectionContext::toInfo)
                .forEach(info -> merged.putIfAbsent(info.getConnectionId(), info));
    }

    /**
     * 从 Redis 收集其他节点的连接信息，跳过本机已收集的 connectionId.
     *
     * @param userId 用户id
     * @param merged 合并结果容器，以 connectionId 为 key 保证去重
     */
    private void collectRedisConnections(final BigInteger userId, final Map<String, ConnectionInfo> merged) {
        try {
            String userKey = userKey(userId);
            Set<String> connectionIdSet = RedisUtils.setMembers(userKey);
            if (connectionIdSet == null || connectionIdSet.isEmpty()) {
                return;
            }
            for (String connectionId : connectionIdSet) {
                if (merged.containsKey(connectionId)) {
                    continue;
                }
                ConnectionInfo connectionInfo = getConnectionInfoFromRedis(connectionId);
                if (Objects.nonNull(connectionInfo)) {
                    merged.putIfAbsent(connectionId, connectionInfo);
                } else {
                    RedisUtils.sRemove(userKey, connectionId);
                }
            }
        } catch (RuntimeException e) {
            log.warn("查询Redis在线状态失败: userId={}", userId, e);
        }
    }

    /**
     * 判断本机是否存在该用户的活跃连接.
     *
     * @param userId 用户id
     * @return 是否在线
     */
    private boolean isLocalOnline(final BigInteger userId) {
        Set<String> localIds = userConnectionMap.get(userId);
        return localIds != null && !localIds.isEmpty();
    }

    /**
     * 判断 Redis 中是否存在该用户的连接记录.
     *
     * @param userId 用户id
     * @return 是否在线
     */
    private boolean isRedisOnline(final BigInteger userId) {
        try {
            Long size = RedisUtils.sSize(userKey(userId));
            return size != null && size > 0;
        } catch (RuntimeException e) {
            log.warn("查询Redis在线状态失败: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 从 Redis 读取连接信息.
     *
     * @param connectionId 连接id
     * @return 连接信息
     */
    private ConnectionInfo getConnectionInfoFromRedis(final String connectionId) {
        String value = RedisUtils.get(connectionKey(connectionId));
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return JSON.parseObject(value, ConnectionInfo.class);
        } catch (JSONException e) {
            log.warn("解析Redis连接信息失败: connectionId={}", connectionId, e);
            return null;
        }
    }

    /**
     * 刷新 Redis 在线状态.
     *
     * @param context 连接上下文
     */
    private void refreshRedisState(final ConnectionContext context) {
        try {
            long ttl = properties.getConnectionTtlSeconds();
            RedisUtils.setEx(connectionKey(context.getConnectionId()), JSON.toJSONString(context.toInfo()), ttl, TimeUnit.SECONDS);
            RedisUtils.sAdd(userKey(context.getUserId()), context.getConnectionId());
            RedisUtils.expire(userKey(context.getUserId()), ttl, TimeUnit.SECONDS);
            RedisUtils.sAdd(serverKey(context.getServerId()), context.getConnectionId());
            RedisUtils.expire(serverKey(context.getServerId()), ttl, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            log.warn("刷新WebSocket在线状态失败: connectionId={}", context.getConnectionId(), e);
        }
    }

    /**
     * 移除 Redis 在线状态.
     *
     * @param context 连接上下文
     */
    private void removeRedisState(final ConnectionContext context) {
        try {
            RedisUtils.delete(connectionKey(context.getConnectionId()));
            cleanupSetMember(userKey(context.getUserId()), context.getConnectionId());
            cleanupSetMember(serverKey(context.getServerId()), context.getConnectionId());
        } catch (RuntimeException e) {
            log.warn("移除WebSocket在线状态失败: connectionId={}", context.getConnectionId(), e);
        }
    }

    /**
     * 清理集合成员并在集合为空时删除集合.
     *
     * @param key          Redis Key
     * @param connectionId 连接id
     */
    private void cleanupSetMember(final String key, final String connectionId) {
        RedisUtils.sRemove(key, connectionId);
        Long size = RedisUtils.sSize(key);
        if (size == null || size <= 0) {
            RedisUtils.delete(key);
        }
    }

    /**
     * 发送响应帧.
     *
     * @param channel Channel
     * @param resp    响应帧
     * @return 是否成功提交写出
     */
    private boolean send(final Channel channel, final WsBaseRespVO<?> resp) {
        return write(channel, resp, false);
    }

    /**
     * 写出响应帧.
     *
     * @param channel         Channel
     * @param resp            响应帧
     * @param closeAfterWrite 写出后是否关闭连接
     * @return 是否成功提交写出
     */
    private boolean write(final Channel channel, final WsBaseRespVO<?> resp, final boolean closeAfterWrite) {
        if (channel == null || !channel.isActive()) {
            return false;
        }
        try {
            TextWebSocketFrame frame = new TextWebSocketFrame(serializeWsFrame(resp));
            if (closeAfterWrite) {
                channel.writeAndFlush(frame).addListener(ChannelFutureListener.CLOSE);
            } else {
                channel.writeAndFlush(frame);
            }
            return true;
        } catch (JSONException e) {
            log.warn("WebSocket响应序列化失败", e);
            channel.close();
            return false;
        }
    }

    /**
     * 序列化WebSocket响应帧.
     *
     * @param resp 响应帧
     * @return JSON文本
     */
    private String serializeWsFrame(final WsBaseRespVO<?> resp) {
        return JSON.toJSONString(
                resp,
                JSONWriter.Feature.BrowserCompatible,
                JSONWriter.Feature.WriteLongAsString
        );
    }

    /**
     * 转换在线连接响应.
     *
     * @param connectionInfo 连接信息
     * @return 在线连接响应
     */
    private OnlineConnectionRespVO toRespVO(final ConnectionInfo connectionInfo) {
        return OnlineConnectionRespVO.builder()
                .connectionId(connectionInfo.getConnectionId())
                .deviceId(connectionInfo.getDeviceId())
                .deviceType(connectionInfo.getDeviceType())
                .ip(connectionInfo.getIp())
                .serverId(connectionInfo.getServerId())
                .connectedAt(connectionInfo.getConnectedAt())
                .lastHeartbeatAt(connectionInfo.getLastHeartbeatAt())
                .build();
    }

    /**
     * 获取连接 Redis Key.
     *
     * @param connectionId 连接id
     * @return Redis Key
     */
    private String connectionKey(final String connectionId) {
        return CacheConstant.generateKey(CacheConstant.WS_CONNECTION, connectionId);
    }

    /**
     * 获取用户在线连接 Redis Key.
     *
     * @param userId 用户id
     * @return Redis Key
     */
    private String userKey(final BigInteger userId) {
        return CacheConstant.generateKey(CacheConstant.WS_USER_CONNECTIONS, userId);
    }

    /**
     * 获取节点在线连接 Redis Key.
     *
     * @param serverId 节点id
     * @return Redis Key
     */
    private String serverKey(final String serverId) {
        return CacheConstant.generateKey(CacheConstant.WS_SERVER_CONNECTIONS, serverId);
    }

    /**
     * 空字符串默认值处理.
     *
     * @param value        原始值
     * @param defaultValue 默认值
     * @return 处理后值
     */
    private String defaultIfBlank(final String value, final String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
