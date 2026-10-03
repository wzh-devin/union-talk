package com.devin.uniontalk.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.user.domain.vo.req.InviteGroupMemberReqVO;
import com.devin.uniontalk.user.domain.vo.resp.GroupMemberRespVO;
import com.devin.uniontalk.user.service.GroupMemberService;
import com.devin.uniontalk.web.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * 群成员(GroupMember)Controller层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "groupMember")
@RestController
@RequiredArgsConstructor
@RequestMapping("/groupMember")
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    /**
     * 邀请用户入群.
     *
     * @param reqVO 邀请入群请求参数
     * @return 邀请结果
     */
    @PostMapping("/invite")
    @Operation(summary = "邀请用户入群")
    public ApiResult<Boolean> inviteMembers(@Valid @RequestBody final InviteGroupMemberReqVO reqVO) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        groupMemberService.inviteMembers(userId, reqVO.getGroupId(), reqVO.getUidList());
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 退出群聊.
     *
     * @param groupId 群聊id
     * @return 退出结果
     */
    @PostMapping("/leave/{groupId}")
    @Operation(summary = "退出群聊")
    public ApiResult<Boolean> leaveGroup(@PathVariable("groupId") final BigInteger groupId) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        groupMemberService.leaveGroup(userId, groupId);
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 踢出群成员.
     *
     * @param groupId  群聊id
     * @param targetId 被踢用户id
     * @return 踢出结果
     */
    @PostMapping("/kick/{groupId}/{targetId}")
    @Operation(summary = "踢出群成员")
    public ApiResult<Boolean> kickMember(@PathVariable("groupId") final BigInteger groupId, @PathVariable("targetId") final BigInteger targetId) {
        BigInteger userId = new BigInteger(StpUtil.getLoginId().toString());
        groupMemberService.kickMember(userId, groupId, targetId);
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 查询群成员列表.
     *
     * @param groupId 群聊id
     * @return 群成员列表
     */
    @GetMapping("/list/{groupId}")
    @Operation(summary = "查询群成员列表")
    public ApiResult<List<GroupMemberRespVO>> getMemberList(@PathVariable("groupId") final BigInteger groupId) {
        return ApiResult.success(groupMemberService.getMemberList(groupId));
    }
}
