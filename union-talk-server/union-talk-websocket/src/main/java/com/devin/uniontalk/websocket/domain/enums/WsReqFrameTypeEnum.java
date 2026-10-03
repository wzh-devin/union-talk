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
 * WebSocket 请求帧类型
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum WsReqFrameTypeEnum {

    /**
     * 客户端心跳请求.
     */
    PING("PING", "客户端心跳");

    /**
     * 帧类型枚举缓存.
     */
    private static final Map<String, WsReqFrameTypeEnum> CACHE;

    static {
        CACHE = Arrays.stream(values())
                .collect(Collectors.toMap(WsReqFrameTypeEnum::getType, Function.identity()));
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
     * 根据类型获取请求帧枚举.
     *
     * @param type 类型
     * @return 请求帧类型
     */
    @JSONCreator
    public static WsReqFrameTypeEnum of(final String type) {
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
