package com.devin.uniontalk.message.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.base.cursor.model.CursorPageQuery;
import com.devin.uniontalk.base.cursor.model.CursorPageResult;
import com.devin.uniontalk.message.domain.vo.req.ConversationPageReqVO;
import com.devin.uniontalk.message.domain.vo.req.ReadConversationReqVO;
import com.devin.uniontalk.message.domain.vo.resp.ConversationRespVO;
import com.devin.uniontalk.message.service.ConversationService;
import com.devin.uniontalk.web.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigInteger;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2026/05/20 15:20:21.
 *
 * <p>
 * 会话表(Conversation)Controller层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "conversation")
@RestController
@RequiredArgsConstructor
@RequestMapping("/conversation")
public class ConversationController {

    /**
     * 会话服务.
     */
    private final ConversationService conversationService;

    /**
     * 查询当前用户会话分页列表.
     *
     * @param reqVO 会话分页查询请求参数
     * @return 当前用户会话分页列表
     */
    @GetMapping("/page")
    @Operation(summary = "查询当前用户会话分页列表")
    public ApiResult<CursorPageResult<ConversationRespVO, Date>> pageConversation(@ParameterObject @Valid final ConversationPageReqVO reqVO) {
        BigInteger userId = currentUserId();
        return ApiResult.success(
                conversationService.pageByUserId(
                        userId,
                        CursorPageQuery.<Date>builder()
                                .cursorValue(reqVO.getCursorValue())
                                .cursorId(reqVO.getCursorId())
                                .pageSize(reqVO.getPageSize())
                                .build()
                )
        );
    }

    /**
     * 查询当前用户会话详情.
     *
     * @param conversationId 会话id
     * @return 当前用户会话详情
     */
    @GetMapping("/{conversationId}")
    @Operation(summary = "查询当前用户会话详情")
    public ApiResult<ConversationRespVO> detailConversation(@PathVariable("conversationId") final BigInteger conversationId) {
        BigInteger userId = currentUserId();
        return ApiResult.success(conversationService.getDetail(userId, conversationId));
    }

    /**
     * 标记会话已读.
     *
     * @param reqVO 会话已读请求参数
     * @return 标记结果
     */
    @PostMapping("/read")
    @Operation(summary = "标记会话已读")
    public ApiResult<Boolean> readConversation(@Valid @RequestBody final ReadConversationReqVO reqVO) {
        BigInteger userId = currentUserId();
        conversationService.read(userId, reqVO.getConversationId(), reqVO.getLastReadMsgId());
        return ApiResult.success(Boolean.TRUE);
    }

    /**
     * 获取当前登录用户id.
     *
     * @return 当前登录用户id
     */
    private BigInteger currentUserId() {
        return new BigInteger(StpUtil.getLoginId().toString());
    }
}
