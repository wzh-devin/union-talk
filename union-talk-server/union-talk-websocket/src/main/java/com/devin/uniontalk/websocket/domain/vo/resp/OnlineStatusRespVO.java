package com.devin.uniontalk.websocket.domain.vo.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * 在线状态响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "在线状态响应参数")
public class OnlineStatusRespVO {

    /**
     * 用户id.
     */
    @Schema(description = "用户id")
    private BigInteger userId;

    /**
     * 用户是否在线.
     */
    @Schema(description = "是否在线")
    private Boolean online;

    /**
     * 用户在线连接数.
     */
    @Schema(description = "在线连接数")
    private Integer connectionCount;

    /**
     * 用户在线连接列表.
     */
    @Schema(description = "连接列表")
    private List<OnlineConnectionRespVO> connectionList;
}
