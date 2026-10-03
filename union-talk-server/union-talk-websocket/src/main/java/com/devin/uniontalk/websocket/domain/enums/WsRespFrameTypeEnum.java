package com.devin.uniontalk.websocket.domain.enums;

import com.alibaba.fastjson2.annotation.JSONCreator;
import com.alibaba.fastjson2.annotation.JSONField;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * WebSocket 响应帧类型
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum WsRespFrameTypeEnum {

    /**
     * 连接成功响应.
     */
    CONNECT_ACK("CONNECT_ACK", "连接成功"),

    /**
     * 服务端心跳响应.
     */
    PONG("PONG", "服务端心跳响应"),

    /**
     * 好友申请创建响应.
     */
    FRIEND_REQUEST_CREATED("FRIEND_REQUEST_CREATED", "好友申请创建响应"),

    /**
     * 好友申请同意响应.
     */
    FRIEND_REQUEST_ACCEPTED("FRIEND_REQUEST_ACCEPTED", "好友申请同意响应"),

    /**
     * 消息创建响应.
     */
    MESSAGE_CREATED("MESSAGE_CREATED", "消息创建响应"),

    /**
     * 会话更新响应.
     */
    CONVERSATION_UPDATED("CONVERSATION_UPDATED", "会话更新响应"),

    /**
     * 错误响应.
     */
    ERROR("ERROR", "错误响应");

    /**
     * 帧类型枚举缓存.
     */
    private static final Map<String, WsRespFrameTypeEnum> CACHE;

    static {
        CACHE = Arrays.stream(values())
                .collect(Collectors.toMap(WsRespFrameTypeEnum::getType, Function.identity()));
    }

    /**
     * 帧类型编码.
     */
    private final String type;

    /**
     * 帧类型描述.
     */
    private final String desc;

    /**
     * 根据类型获取响应帧枚举.
     *
     * @param type 类型
     * @return 响应帧类型
     */
    @JSONCreator
    public static WsRespFrameTypeEnum of(final String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        return CACHE.get(type.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * 获取 JSON 协议中的帧类型编码.
     *
     * @return 帧类型编码
     */
    @JSONField(value = true)
    public String getType() {
        return type;
    }
}
