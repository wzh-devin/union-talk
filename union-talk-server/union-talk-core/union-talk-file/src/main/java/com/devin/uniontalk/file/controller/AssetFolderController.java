package com.devin.uniontalk.file.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.file.domain.vo.req.CreateFolderReqVO;
import com.devin.uniontalk.file.domain.vo.req.MoveFolderReqVO;
import com.devin.uniontalk.file.domain.vo.req.RenameFolderReqVO;
import com.devin.uniontalk.file.domain.vo.resp.AssetFolderTreeRespVO;
import com.devin.uniontalk.file.service.AssetFolderService;
import com.devin.uniontalk.web.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2026/06/30 19:05.
 *
 * <p>
 * 资产目录(AssetFolder)Controller层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "folder")
@RestController
@RequiredArgsConstructor
@RequestMapping("/asset/folder")
public class AssetFolderController {

    /**
     * 资产目录服务.
     */
    private final AssetFolderService assetFolderService;

    /**
     * 创建目录.
     *
     * @param reqVO 创建目录请求参数
     * @return 目录响应参数
     */
    @PostMapping("/create")
    @Operation(summary = "创建目录")
    public ApiResult<AssetFolderTreeRespVO> create(@Valid @RequestBody final CreateFolderReqVO reqVO) {
        return ApiResult.success(assetFolderService.create(
                currentUserId(),
                reqVO.getConversationId(),
                reqVO.getParentId(),
                reqVO.getName()
        ));
    }

    /**
     * 查询目录树.
     *
     * @param conversationId 会话id
     * @return 目录树
     */
    @GetMapping("/tree")
    @Operation(summary = "查询目录树")
    public ApiResult<AssetFolderTreeRespVO> getTree(
            @Parameter(name = "conversationId", description = "会话id", required = true)
            @RequestParam("conversationId") final BigInteger conversationId
    ) {
        return ApiResult.success(assetFolderService.getTree(currentUserId(), conversationId));
    }

    /**
     * 移动目录.
     *
     * @param folderId 目录id
     * @param reqVO    移动目录请求参数
     * @return 目录树
     */
    @PatchMapping("/{folderId}/move")
    @Operation(summary = "移动目录")
    public ApiResult<AssetFolderTreeRespVO> move(
            @Parameter(name = "folderId", description = "目录id", required = true)
            @PathVariable("folderId") final BigInteger folderId,
            @Valid @RequestBody final MoveFolderReqVO reqVO
    ) {
        return ApiResult.success(assetFolderService.move(currentUserId(), folderId, reqVO.getTargetParentId()));
    }

    /**
     * 重命名目录.
     *
     * @param folderId 目录id
     * @param reqVO    重命名目录请求参数
     * @return 目录树
     */
    @PatchMapping("/{folderId}/rename")
    @Operation(summary = "重命名目录")
    public ApiResult<AssetFolderTreeRespVO> rename(
            @Parameter(name = "folderId", description = "目录id", required = true)
            @PathVariable("folderId") final BigInteger folderId,
            @Valid @RequestBody final RenameFolderReqVO reqVO
    ) {
        return ApiResult.success(assetFolderService.rename(currentUserId(), folderId, reqVO.getName()));
    }

    /**
     * 删除目录.
     *
     * @param folderId 目录id
     * @return 通用响应
     */
    @DeleteMapping("/{folderId}")
    @Operation(summary = "删除目录")
    public ApiResult<Void> delete(
            @Parameter(name = "folderId", description = "目录id", required = true)
            @PathVariable("folderId") final BigInteger folderId
    ) {
        assetFolderService.delete(currentUserId(), folderId);
        return ApiResult.success();
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
