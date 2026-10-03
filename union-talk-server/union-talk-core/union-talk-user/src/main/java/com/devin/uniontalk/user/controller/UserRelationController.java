package com.devin.uniontalk.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.user.domain.vo.req.MoveFriendGroupReqVO;
import com.devin.uniontalk.user.domain.vo.req.UpdateRemarkReqVO;
import com.devin.uniontalk.user.domain.vo.resp.FriendRespVO;
import com.devin.uniontalk.user.service.UserRelationService;
import com.devin.uniontalk.web.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigInteger;
import java.util.List;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 用户关系(UserRelation)Controller层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "userRelation")
@RestController
@RequiredArgsConstructor
@RequestMapping("/userRelation")
public class UserRelationController {

    private final UserRelationService userRelationService;

    /**
     * 查询好友列表.
     *
     * @return 好友列表
     */
    @GetMapping("/friend/list")
    @Operation(summary = "查询好友列表")
    public ApiResult<List<FriendRespVO>> getFriendList() {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        return ApiResult.success(userRelationService.getFriendList(userId));
    }

    /**
     * 修改好友备注.
     *
     * @param reqVO 修改备注请求参数
     * @return 修改结果
     */
    @PutMapping("/remark")
    @Operation(summary = "修改好友备注")
    public ApiResult<Boolean> updateRemark(@Valid @RequestBody final UpdateRemarkReqVO reqVO) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        userRelationService.updateRemark(userId, reqVO.getTargetId(), reqVO.getRemark());
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 移动好友到指定分组.
     *
     * @param reqVO 移动分组请求参数
     * @return 移动结果
     */
    @PutMapping("/moveGroup")
    @Operation(summary = "移动好友分组")
    public ApiResult<Boolean> moveGroup(@Valid @RequestBody final MoveFriendGroupReqVO reqVO) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        userRelationService.moveFriendGroup(userId, reqVO.getTargetId(), reqVO.getFriendGroupId());
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 删除好友.
     *
     * @param targetId 对方id
     * @return 删除结果
     */
    @DeleteMapping("/delete/{targetId}")
    @Operation(summary = "删除好友")
    public ApiResult<Boolean> deleteFriend(@PathVariable("targetId") final BigInteger targetId) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        userRelationService.deleteFriend(userId, targetId);
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 拉黑好友.
     *
     * @param targetId 对方id
     * @return 拉黑结果
     */
    @PostMapping("/block/{targetId}")
    @Operation(summary = "拉黑好友")
    public ApiResult<Boolean> blockFriend(@PathVariable("targetId") final BigInteger targetId) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        userRelationService.blockFriend(userId, targetId);
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 取消拉黑.
     *
     * @param targetId 对方id
     * @return 取消拉黑结果
     */
    @PostMapping("/unblock/{targetId}")
    @Operation(summary = "取消拉黑")
    public ApiResult<Boolean> unblockFriend(@PathVariable("targetId") final BigInteger targetId) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        userRelationService.unblockFriend(userId, targetId);
        return ApiResult.success(Boolean.TRUE);
    }
}
