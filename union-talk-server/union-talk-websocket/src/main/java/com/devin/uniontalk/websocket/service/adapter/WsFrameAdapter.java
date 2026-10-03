package com.devin.uniontalk.websocket.service.adapter;

import com.devin.uniontalk.rabbitmq.domain.event.ConversationUpdatedEvent;
import com.devin.uniontalk.rabbitmq.domain.event.FriendRequestAcceptedEvent;
import com.devin.uniontalk.rabbitmq.domain.event.FriendRequestCreatedEvent;
import com.devin.uniontalk.rabbitmq.domain.event.MessageCreatedEvent;
import com.devin.uniontalk.web.response.ResultEnum;
import com.devin.uniontalk.websocket.domain.enums.WsRespFrameTypeEnum;
import com.devin.uniontalk.websocket.domain.model.ConnectionInfo;
import com.devin.uniontalk.websocket.domain.vo.resp.WsBaseRespVO;
import com.devin.uniontalk.websocket.domain.vo.resp.WsConversationUpdatedRespVO;
import com.devin.uniontalk.websocket.domain.vo.resp.WsFriendRequestAcceptedRespVO;
import com.devin.uniontalk.websocket.domain.vo.resp.WsFriendRequestCreatedRespVO;
import com.devin.uniontalk.websocket.domain.vo.resp.WsMessageCreatedRespVO;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * WebSocket 帧适配器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class WsFrameAdapter {

    private WsFrameAdapter() {
    }

    /**
     * 构建连接成功响应.
     *
     * @param connectionInfo 连接信息
     * @return WebSocket响应帧
     */
    public static WsBaseRespVO<ConnectionInfo> connectAck(final ConnectionInfo connectionInfo) {
        return connectAck(null, connectionInfo);
    }

    /**
     * 构建连接成功响应.
     *
     * @param requestId      请求id
     * @param connectionInfo 连接信息
     * @return WebSocket响应帧
     */
    public static WsBaseRespVO<ConnectionInfo> connectAck(
            final String requestId,
            final ConnectionInfo connectionInfo
    ) {
        return success(WsRespFrameTypeEnum.CONNECT_ACK, requestId, connectionInfo);
    }

    /**
     * 构建心跳响应.
     *
     * @param requestId 请求id
     * @return WebSocket响应帧
     */
    public static WsBaseRespVO<Map<String, Long>> pong(final String requestId) {
        return success(
                WsRespFrameTypeEnum.PONG,
                requestId,
                Map.of("serverTime", System.currentTimeMillis())
        );
    }

    /**
     * 构建好友申请创建响应.
     *
     * @param event 好友申请创建事件
     * @return WebSocket好友申请创建响应帧
     */
    public static WsFriendRequestCreatedRespVO friendRequestCreated(final FriendRequestCreatedEvent event) {
        WsFriendRequestCreatedRespVO resp = new WsFriendRequestCreatedRespVO();
        fillSuccess(resp, WsRespFrameTypeEnum.FRIEND_REQUEST_CREATED);
        resp.setData(buildFriendRequestCreatedData(event));
        return resp;
    }

    /**
     * 构建好友申请同意响应.
     *
     * @param event 好友申请同意事件
     * @return WebSocket好友申请同意响应帧
     */
    public static WsFriendRequestAcceptedRespVO friendRequestAccepted(final FriendRequestAcceptedEvent event) {
        WsFriendRequestAcceptedRespVO resp = new WsFriendRequestAcceptedRespVO();
        fillSuccess(resp, WsRespFrameTypeEnum.FRIEND_REQUEST_ACCEPTED);
        resp.setData(buildFriendRequestAcceptedData(event));
        return resp;
    }

    /**
     * 构建消息创建响应.
     *
     * @param event 消息创建事件
     * @return WebSocket消息创建响应帧
     */
    public static WsMessageCreatedRespVO messageCreated(final MessageCreatedEvent event) {
        WsMessageCreatedRespVO resp = new WsMessageCreatedRespVO();
        fillSuccess(resp, WsRespFrameTypeEnum.MESSAGE_CREATED);
        resp.setData(buildMessageCreatedData(event));
        return resp;
    }

    /**
     * 构建会话更新响应.
     *
     * @param event 会话更新事件
     * @return WebSocket会话更新响应帧
     */
    public static WsConversationUpdatedRespVO conversationUpdated(final ConversationUpdatedEvent event) {
        WsConversationUpdatedRespVO resp = new WsConversationUpdatedRespVO();
        fillSuccess(resp, WsRespFrameTypeEnum.CONVERSATION_UPDATED);
        resp.setData(buildConversationUpdatedData(event));
        return resp;
    }

    /**
     * 构建好友申请创建响应数据.
     *
     * @param event 好友申请创建事件
     * @return WebSocket好友申请创建响应数据
     */
    private static WsFriendRequestCreatedRespVO.WsFriendRequestCreatedRespData buildFriendRequestCreatedData(
            final FriendRequestCreatedEvent event
    ) {
        WsFriendRequestCreatedRespVO.WsFriendRequestCreatedRespData data =
                new WsFriendRequestCreatedRespVO.WsFriendRequestCreatedRespData();
        data.setFriendRequestId(event.getFriendRequestId());
        data.setFromUserId(event.getFromUserId());
        data.setToUserId(event.getToUserId());
        data.setFromUsername(event.getFromUsername());
        data.setFromAvatarUrl(event.getFromAvatarUrl());
        data.setApplyMsg(event.getApplyMsg());
        data.setStatus(event.getStatus());
        data.setCreatedAt(event.getCreatedAt());
        return data;
    }

    /**
     * 构建好友申请同意响应数据.
     *
     * @param event 好友申请同意事件
     * @return WebSocket好友申请同意响应数据
     */
    private static WsFriendRequestAcceptedRespVO.WsFriendRequestAcceptedRespData buildFriendRequestAcceptedData(
            final FriendRequestAcceptedEvent event
    ) {
        WsFriendRequestAcceptedRespVO.WsFriendRequestAcceptedRespData data =
                new WsFriendRequestAcceptedRespVO.WsFriendRequestAcceptedRespData();
        data.setFriendRequestId(event.getFriendRequestId());
        data.setFromUserId(event.getFromUserId());
        data.setToUserId(event.getToUserId());
        data.setToUsername(event.getToUsername());
        data.setToAvatarUrl(event.getToAvatarUrl());
        data.setConversationId(event.getConversationId());
        data.setStatus(event.getStatus());
        data.setHandledAt(event.getHandledAt());
        data.setContent(event.getContent());
        return data;
    }

    /**
     * 构建消息创建响应数据.
     *
     * @param event 消息创建事件
     * @return WebSocket消息创建响应数据
     */
    private static WsMessageCreatedRespVO.WsMessageCreatedRespData buildMessageCreatedData(
            final MessageCreatedEvent event
    ) {
        WsMessageCreatedRespVO.WsMessageCreatedRespData data = new WsMessageCreatedRespVO.WsMessageCreatedRespData();
        data.setMessageId(event.getMessageId());
        data.setConversationId(event.getConversationId());
        data.setSenderType(event.getSenderType());
        data.setSenderUser(buildSenderUserData(event.getSenderUser()));
        data.setSenderAgent(buildSenderAgentData(event.getSenderAgent()));
        data.setType(event.getType());
        data.setContent(event.getContent());
        data.setAssetInfo(buildAssetInfoData(event.getAssetInfo()));
        data.setQuoteMsgId(event.getQuoteMsgId());
        data.setMentionList(Objects.requireNonNullElse(
                        event.getMentionList(),
                        List.<MessageCreatedEvent.MentionSnapshot>of()
                )
                .stream()
                .map(WsFrameAdapter::buildMentionData)
                .toList());
        data.setAgentRunId(event.getAgentRunId());
        data.setTriggerMessageId(event.getTriggerMessageId());
        data.setCitationList(
                Objects.requireNonNullElse(event.getCitationList(),
                                List.<MessageCreatedEvent.AgentCitationSnapshot>of()
                        )
                        .stream()
                        .map(WsFrameAdapter::buildAgentCitationData)
                        .toList());
        data.setRecalled(event.getRecalled());
        data.setCreatedAt(event.getCreatedAt());
        return data;
    }

    /**
     * 构建消息提及响应数据.
     *
     * @param mention 消息提及快照
     * @return 消息提及响应数据
     */
    private static WsMessageCreatedRespVO.MentionData buildMentionData(
            final MessageCreatedEvent.MentionSnapshot mention
    ) {
        WsMessageCreatedRespVO.MentionData data = new WsMessageCreatedRespVO.MentionData();
        data.setMentionType(mention.getMentionType());
        data.setTargetId(mention.getTargetId());
        data.setDisplayText(mention.getDisplayText());
        data.setStartOffset(mention.getStartOffset());
        data.setLength(mention.getLength());
        return data;
    }

    /**
     * 构建发送者Agent响应数据.
     *
     * @param senderAgent 发送者Agent快照
     * @return 发送者Agent响应数据
     */
    private static WsMessageCreatedRespVO.SenderAgentData buildSenderAgentData(
            final MessageCreatedEvent.SenderAgentSnapshot senderAgent
    ) {
        if (Objects.isNull(senderAgent)) {
            return null;
        }
        WsMessageCreatedRespVO.SenderAgentData data = new WsMessageCreatedRespVO.SenderAgentData();
        data.setAgentId(senderAgent.getAgentId());
        data.setDisplayName(senderAgent.getDisplayName());
        return data;
    }

    /**
     * 构建Agent引用响应数据.
     *
     * @param citation Agent引用快照
     * @return Agent引用响应数据
     */
    private static WsMessageCreatedRespVO.AgentCitationData buildAgentCitationData(
            final MessageCreatedEvent.AgentCitationSnapshot citation
    ) {
        WsMessageCreatedRespVO.AgentCitationData data = new WsMessageCreatedRespVO.AgentCitationData();
        data.setCitationKey(citation.getCitationKey());
        data.setSourceType(citation.getSourceType());
        data.setMessageId(citation.getMessageId());
        data.setAssetFileId(citation.getAssetFileId());
        data.setResourceVersion(citation.getResourceVersion());
        data.setChunkId(citation.getChunkId());
        data.setPageFrom(citation.getPageFrom());
        data.setPageTo(citation.getPageTo());
        data.setHeadingPath(citation.getHeadingPath());
        return data;
    }

    /**
     * 构建消息资产文件响应数据.
     *
     * @param assetInfo 消息资产文件快照
     * @return 消息资产文件响应数据
     */
    private static WsMessageCreatedRespVO.AssetInfoData buildAssetInfoData(
            final MessageCreatedEvent.AssetInfoSnapshot assetInfo
    ) {
        if (Objects.isNull(assetInfo)) {
            return null;
        }
        WsMessageCreatedRespVO.AssetInfoData data = new WsMessageCreatedRespVO.AssetInfoData();
        data.setId(assetInfo.getId());
        data.setConversationId(assetInfo.getConversationId());
        data.setName(assetInfo.getName());
        data.setFileExt(assetInfo.getFileExt());
        data.setFileType(assetInfo.getFileType());
        data.setFileSize(assetInfo.getFileSize());
        data.setMimeType(assetInfo.getMimeType());
        return data;
    }

    /**
     * 构建发送者用户响应数据.
     *
     * @param senderUser 发送者用户快照
     * @return 发送者用户响应数据
     */
    private static WsMessageCreatedRespVO.SenderUserData buildSenderUserData(
            final MessageCreatedEvent.SenderUserSnapshot senderUser
    ) {
        if (Objects.isNull(senderUser)) {
            return null;
        }
        WsMessageCreatedRespVO.SenderUserData data = new WsMessageCreatedRespVO.SenderUserData();
        data.setUserId(senderUser.getUserId());
        data.setCode(senderUser.getCode());
        data.setUsername(senderUser.getUsername());
        data.setAvatarUrl(senderUser.getAvatarUrl());
        data.setStatus(senderUser.getStatus());
        return data;
    }

    /**
     * 构建会话更新响应数据.
     *
     * @param event 会话更新事件
     * @return WebSocket会话更新响应数据
     */
    private static WsConversationUpdatedRespVO.WsConversationUpdatedRespData buildConversationUpdatedData(
            final ConversationUpdatedEvent event
    ) {
        WsConversationUpdatedRespVO.WsConversationUpdatedRespData data =
                new WsConversationUpdatedRespVO.WsConversationUpdatedRespData();
        data.setConversationId(event.getConversationId());
        data.setType(event.getType());
        data.setGroupId(event.getGroupId());
        data.setLastMsgId(event.getLastMsgId());
        data.setLastMsgAt(event.getLastMsgAt());
        data.setSenderId(event.getSenderId());
        data.setSenderType(event.getSenderType());
        data.setUpdatedAt(event.getUpdatedAt());
        return data;
    }

    /**
     * 构建成功响应.
     *
     * @param type      帧类型
     * @param requestId 请求id
     * @param data      响应数据
     * @param <T>       数据类型
     * @return WebSocket响应帧
     */
    public static <T> WsBaseRespVO<T> success(
            final WsRespFrameTypeEnum type,
            final String requestId,
            final T data
    ) {
        WsBaseRespVO<T> resp = new WsBaseRespVO<>();
        resp.setType(type);
        resp.setRequestId(requestId);
        resp.setSuccess(Boolean.TRUE);
        resp.setCode(ResultEnum.SUCCESS.getCode());
        resp.setMessage(ResultEnum.SUCCESS.getMessage());
        resp.setTimestamp(System.currentTimeMillis());
        resp.setData(data);
        return resp;
    }

    /**
     * 构建失败响应.
     *
     * @param requestId 请求id
     * @param code      错误码
     * @param message   错误信息
     * @return WebSocket响应帧
     */
    public static WsBaseRespVO<Void> error(final String requestId, final Integer code, final String message) {
        WsBaseRespVO<Void> resp = new WsBaseRespVO<>();
        resp.setType(WsRespFrameTypeEnum.ERROR);
        resp.setRequestId(requestId);
        resp.setSuccess(Boolean.FALSE);
        resp.setCode(code);
        resp.setMessage(message);
        resp.setTimestamp(System.currentTimeMillis());
        return resp;
    }

    /**
     * 填充成功响应基础字段.
     *
     * @param resp 响应帧
     * @param type 响应帧类型
     */
    private static void fillSuccess(final WsBaseRespVO<?> resp, final WsRespFrameTypeEnum type) {
        resp.setType(type);
        resp.setSuccess(Boolean.TRUE);
        resp.setCode(ResultEnum.SUCCESS.getCode());
        resp.setMessage(ResultEnum.SUCCESS.getMessage());
        resp.setTimestamp(System.currentTimeMillis());
    }
}
