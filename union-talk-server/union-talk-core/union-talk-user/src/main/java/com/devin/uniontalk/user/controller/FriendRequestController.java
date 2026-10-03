package com.devin.uniontalk.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.user.domain.vo.req.HandleFriendRequestReqVO;
import com.devin.uniontalk.user.domain.vo.req.SendFriendRequestReqVO;
import com.devin.uniontalk.user.domain.vo.resp.FriendRequestRespVO;
import com.devin.uniontalk.user.service.FriendRequestService;
import com.devin.uniontalk.web.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigInteger;
import java.util.List;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 好友申请(FriendRequest)Controller层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "friendRequest")
@RestController
@RequiredArgsConstructor
@RequestMapping("/friendRequest")
public class FriendRequestController {

    private final FriendRequestService friendRequestService;

    /**
     * 发送好友申请.
     *
     * @param reqVO 发送好友申请请求参数
     * @return 发送结果
     */
    @PostMapping("/send")
    @Operation(summary = "发送好友申请")
    public ApiResult<Boolean> sendRequest(@Valid @RequestBody final SendFriendRequestReqVO reqVO) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        friendRequestService.sendRequest(userId, reqVO.getToUserCode(), reqVO.getApplyMsg());
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 处理好友申请.
     *
     * @param reqVO 处理好友申请请求参数
     * @return 处理结果
     */
    @PostMapping("/handle")
    @Operation(summary = "处理好友申请")
    public ApiResult<Boolean> handleRequest(@Valid @RequestBody final HandleFriendRequestReqVO reqVO) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        friendRequestService.handleRequest(
                userId,
                reqVO.getRequestId(),
                reqVO.getAccept(),
                reqVO.getFriendGroupId(),
                reqVO.getRemark()
        );
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 查询收到的好友申请列表.
     *
     * @return 好友申请列表
     */
    @GetMapping("/received/list")
    @Operation(summary = "查询收到的好友申请列表")
    public ApiResult<List<FriendRequestRespVO>> getReceivedRequestList() {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        return ApiResult.success(friendRequestService.getReceivedRequestList(userId));
    }

    /**
     * 查询发出的好友申请列表.
     *
     * @return 好友申请列表
     */
    @GetMapping("/sent/list")
    @Operation(summary = "查询发出的好友申请列表")
    public ApiResult<List<FriendRequestRespVO>> getSentRequestList() {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        return ApiResult.success(friendRequestService.getSentRequestList(userId));
    }
}
