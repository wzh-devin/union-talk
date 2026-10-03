package com.devin.uniontalk.message.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.base.cursor.model.CursorPageQuery;
import com.devin.uniontalk.base.cursor.model.CursorPageResult;
import com.devin.uniontalk.message.domain.entity.convertor.MessageConvertor;
import com.devin.uniontalk.message.domain.vo.req.MessagePageReqVO;
import com.devin.uniontalk.message.domain.vo.req.SendMessageReqVO;
import com.devin.uniontalk.message.domain.vo.resp.MessageRespVO;
import com.devin.uniontalk.message.service.MessageService;
import com.devin.uniontalk.web.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2026/05/20 15:20:24.
 *
 * <p>
 * 消息表(Message)Controller层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "message")
@RestController
@RequiredArgsConstructor
@RequestMapping("")
public class MessageController {

    /**
     * 消息服务.
     */
    private final MessageService messageService;

    /**
     * 发送消息.
     *
     * @param reqVO 发送消息请求参数
     * @return 消息响应参数
     */
    @PostMapping("/send")
    @Operation(summary = "发送消息")
    public ApiResult<MessageRespVO> sendMessage(@Valid @RequestBody final SendMessageReqVO reqVO) {
        BigInteger senderId = currentUserId();
        return ApiResult.success(
                messageService.send(
                        senderId,
                        reqVO.getConversationId(),
                        reqVO.getType(),
                        reqVO.getContent(),
                        reqVO.getQuoteMsgId(),
                        Objects.isNull(reqVO.getMentionList())
                                ? List.of()
                                : MessageConvertor.INSTANCE.toMentionCommandList(reqVO.getMentionList())
                )
        );
    }

    /**
     * 查询会话消息分页列表.
     *
     * @param reqVO 消息分页查询请求参数
     * @return 会话消息分页列表
     */
    @GetMapping("/page")
    @Operation(summary = "查询会话消息分页列表")
    public ApiResult<CursorPageResult<MessageRespVO, Date>> pageMessage(@ParameterObject @Valid final MessagePageReqVO reqVO) {
        BigInteger userId = currentUserId();
        return ApiResult.success(messageService.pageByConversationId(
                userId,
                reqVO.getConversationId(),
                CursorPageQuery.<Date>builder()
                        .cursorValue(reqVO.getCursorValue())
                        .cursorId(reqVO.getCursorId())
                        .pageSize(reqVO.getPageSize())
                        .build()
        ));
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
