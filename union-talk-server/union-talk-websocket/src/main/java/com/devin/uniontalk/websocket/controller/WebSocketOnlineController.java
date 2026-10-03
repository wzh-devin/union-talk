package com.devin.uniontalk.websocket.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.web.response.ApiResult;
import com.devin.uniontalk.websocket.domain.vo.req.OnlineCheckReqVO;
import com.devin.uniontalk.websocket.domain.vo.resp.OnlineStatusRespVO;
import com.devin.uniontalk.websocket.service.WebSocketConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigInteger;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * WebSocket 在线状态接口
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "WebSocketOnline")
@RestController
@RequiredArgsConstructor
@RequestMapping("/websocket/online")
public class WebSocketOnlineController {

    /**
     * WebSocket 连接服务.
     */
    private final WebSocketConnectionService webSocketConnectionService;

    /**
     * 查询当前用户在线连接.
     *
     * @return 当前用户在线状态
     */
    @GetMapping("/me")
    @Operation(summary = "查询当前用户在线连接")
    public ApiResult<OnlineStatusRespVO> me() {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        return ApiResult.success(webSocketConnectionService.getOnlineStatus(userId));
    }

    /**
     * 批量检查用户在线状态.
     *
     * @param reqVO 查询参数
     * @return 在线状态Map
     */
    @PostMapping("/check")
    @Operation(summary = "批量检查用户在线状态")
    public ApiResult<Map<BigInteger, Boolean>> check(@Valid @RequestBody final OnlineCheckReqVO reqVO) {
        return ApiResult.success(webSocketConnectionService.checkOnline(reqVO.getUserIdList()));
    }
}
