package com.devin.uniontalk.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.user.domain.vo.req.CreateGroupReqVO;
import com.devin.uniontalk.user.domain.vo.req.UpdateGroupReqVO;
import com.devin.uniontalk.user.domain.vo.resp.GroupRespVO;
import com.devin.uniontalk.user.service.GroupService;
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

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 * 群聊(Group)Controller层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "group")
@RestController
@RequiredArgsConstructor
@RequestMapping("/group")
public class GroupController {

    /**
     * 群聊服务.
     */
    private final GroupService groupService;

    /**
     * 创建群聊.
     *
     * @param reqVO 创建群聊请求参数
     * @return 创建的群聊信息
     */
    @PostMapping("/create")
    @Operation(summary = "创建群聊")
    public ApiResult<GroupRespVO> createGroup(@Valid @RequestBody final CreateGroupReqVO reqVO) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        GroupRespVO result = groupService.createGroup(
                userId,
                reqVO.getName(),
                reqVO.getAvatarUrl(),
                reqVO.getDescription(),
                reqVO.getMemberLimit(),
                reqVO.getUidList()
        );
        return ApiResult.success(result);
    }

    /**
     * 更新群聊信息.
     *
     * @param groupId 群聊id
     * @param reqVO   更新群聊请求参数
     * @return 更新结果
     */
    @PutMapping("/update/{groupId}")
    @Operation(summary = "更新群聊信息")
    public ApiResult<Boolean> updateGroup(
            @PathVariable("groupId") final BigInteger groupId,
            @Valid @RequestBody final UpdateGroupReqVO reqVO
    ) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        groupService.updateGroup(
                userId,
                groupId,
                reqVO.getName(),
                reqVO.getAvatarUrl(),
                reqVO.getDescription(),
                reqVO.getMemberLimit()
        );
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 解散群聊.
     *
     * @param groupId 群聊id
     * @return 解散结果
     */
    @DeleteMapping("/dissolve/{groupId}")
    @Operation(summary = "解散群聊")
    public ApiResult<Boolean> dissolveGroup(@PathVariable("groupId") final BigInteger groupId) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        groupService.dissolveGroup(userId, groupId);
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 获取群聊详情.
     *
     * @param groupId 群聊id
     * @return 群聊详情
     */
    @GetMapping("/detail/{groupId}")
    @Operation(summary = "获取群聊详情")
    public ApiResult<GroupRespVO> getGroupDetail(@PathVariable("groupId") final BigInteger groupId) {
        return ApiResult.success(groupService.getGroupDetail(groupId));
    }
}
