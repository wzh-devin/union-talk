package com.devin.uniontalk.websocket.service;

import com.devin.uniontalk.websocket.domain.vo.resp.OnlineStatusRespVO;
import com.devin.uniontalk.websocket.domain.vo.resp.WsBaseRespVO;
import io.netty.channel.Channel;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * WebSocket 连接服务
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface WebSocketConnectionService {

    /**
     * WebSocket 握手完成后执行鉴权并注册连接.
     *
     * @param channel Channel
     */
    void authorize(Channel channel);

    /**
     * 移除连接.
     *
     * @param channel Channel
     */
    void remove(Channel channel);

    /**
     * 处理心跳.
     *
     * @param channel   Channel
     * @param requestId 请求id
     */
    void handleHeartbeat(Channel channel, String requestId);

    /**
     * 发送错误帧并关闭连接.
     *
     * @param channel   Channel
     * @param requestId 请求id
     * @param code      错误码
     * @param message   错误信息
     */
    void sendErrorAndClose(Channel channel, String requestId, Integer code, String message);

    /**
     * 发送错误帧.
     *
     * @param channel   Channel
     * @param requestId 请求id
     * @param code      错误码
     * @param message   错误信息
     */
    void sendError(Channel channel, String requestId, Integer code, String message);

    /**
     * 发送消息给本机指定用户的所有连接.
     *
     * @param userId 用户id
     * @param resp   响应帧
     * @return 已写入响应帧的本机连接数量
     */
    int sendToUser(BigInteger userId, WsBaseRespVO<?> resp);

    /**
     * 查询用户在线状态.
     *
     * @param userId 用户id
     * @return 在线状态
     */
    OnlineStatusRespVO getOnlineStatus(BigInteger userId);

    /**
     * 批量检查用户是否在线.
     *
     * @param userIdList 用户id列表
     * @return 在线状态Map
     */
    Map<BigInteger, Boolean> checkOnline(Collection<BigInteger> userIdList);
}
