package com.devin.uniontalk.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.user.domain.entity.FriendGroup;
import com.devin.uniontalk.user.domain.entity.convertor.FriendGroupConvertor;
import com.devin.uniontalk.user.domain.vo.req.CreateFriendGroupReqVO;
import com.devin.uniontalk.user.domain.vo.req.UpdateFriendGroupReqVO;
import com.devin.uniontalk.user.domain.vo.resp.FriendGroupRespVO;
import com.devin.uniontalk.user.service.FriendGroupService;
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
 * 好友分组(FriendGroup)Controller层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "friendGroup")
@RestController
@RequiredArgsConstructor
@RequestMapping("/friendGroup")
public class FriendGroupController {

    private final FriendGroupService friendGroupService;

    /**
     * 创建好友分组.
     *
     * @param reqVO 创建好友分组请求参数
     * @return 创建的好友分组信息
     */
    @PostMapping("/create")
    @Operation(summary = "创建好友分组")
    public ApiResult<FriendGroupRespVO> createFriendGroup(@Valid @RequestBody final CreateFriendGroupReqVO reqVO) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        FriendGroup group = friendGroupService.createFriendGroup(userId, reqVO.getName(), reqVO.getSortOrder());
        return ApiResult.success(FriendGroupConvertor.INSTANCE.toRespVO(group));
    }

    /**
     * 查询好友分组列表.
     *
     * @return 好友分组列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询好友分组列表")
    public ApiResult<List<FriendGroupRespVO>> getFriendGroupList() {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        List<FriendGroupRespVO> result = FriendGroupConvertor.INSTANCE.toRespVOList(friendGroupService.getFriendGroupList(userId));
        return ApiResult.success(result);
    }

    /**
     * 更新好友分组.
     *
     * @param groupId 分组id
     * @param reqVO   更新好友分组请求参数
     * @return 更新结果
     */
    @PutMapping("/update/{groupId}")
    @Operation(summary = "更新好友分组")
    public ApiResult<Boolean> updateFriendGroup(
            @PathVariable("groupId") final BigInteger groupId,
            @Valid @RequestBody final UpdateFriendGroupReqVO reqVO
    ) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        friendGroupService.updateFriendGroup(userId, groupId, reqVO.getName(), reqVO.getSortOrder());
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 删除好友分组.
     *
     * @param groupId 分组id
     * @return 删除结果
     */
    @DeleteMapping("/delete/{groupId}")
    @Operation(summary = "删除好友分组")
    public ApiResult<Boolean> deleteFriendGroup(@PathVariable("groupId") final BigInteger groupId) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        friendGroupService.deleteFriendGroup(userId, groupId);
        return ApiResult.success(Boolean.TRUE);
    }
}
