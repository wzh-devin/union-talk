package com.devin.uniontalk.file.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.file.domain.vo.req.MoveFileReqVO;
import com.devin.uniontalk.file.domain.vo.req.RenameFileReqVO;
import com.devin.uniontalk.file.domain.vo.resp.AssetFileRespVO;
import com.devin.uniontalk.file.service.AssetFileService;
import com.devin.uniontalk.web.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigInteger;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2026/06/30 19:05.
 *
 * <p>
 * 资产文件(AssetFile)Controller层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "assets")
@RestController
@RequiredArgsConstructor
@RequestMapping("/asset/file")
public class AssetFileController {

    /**
     * 资产文件服务.
     */
    private final AssetFileService assetFileService;

    /**
     * 查询目录下文件列表.
     *
     * @param conversationId 会话id
     * @param folderId       文件夹id
     * @return 文件列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询目录下文件列表")
    public ApiResult<List<AssetFileRespVO>> getFileList(
            @Parameter(name = "conversationId", description = "会话id", required = true)
            @RequestParam("conversationId") final BigInteger conversationId,
            @Parameter(name = "folderId", description = "文件夹id，不传时使用会话根目录")
            @RequestParam(value = "folderId", required = false) final BigInteger folderId
    ) {
        return ApiResult.success(assetFileService.getFileList(currentUserId(), conversationId, folderId));
    }

    /**
     * 移动文件.
     *
     * @param fileId 文件id
     * @param reqVO  移动文件请求参数
     * @return 文件响应参数
     */
    @PatchMapping("/{fileId}/move")
    @Operation(summary = "移动文件")
    public ApiResult<AssetFileRespVO> move(
            @Parameter(name = "fileId", description = "文件id", required = true)
            @PathVariable("fileId") final BigInteger fileId,
            @Valid @RequestBody final MoveFileReqVO reqVO
    ) {
        return ApiResult.success(assetFileService.move(currentUserId(), fileId, reqVO.getTargetFolderId()));
    }

    /**
     * 重命名文件.
     *
     * @param fileId 文件id
     * @param reqVO  重命名文件请求参数
     * @return 文件响应参数
     */
    @PatchMapping("/{fileId}/rename")
    @Operation(summary = "重命名文件")
    public ApiResult<AssetFileRespVO> rename(
            @Parameter(name = "fileId", description = "文件id", required = true)
            @PathVariable("fileId") final BigInteger fileId,
            @Valid @RequestBody final RenameFileReqVO reqVO
    ) {
        return ApiResult.success(assetFileService.rename(currentUserId(), fileId, reqVO.getName()));
    }

    /**
     * 获取文件预览地址.
     *
     * @param fileId 文件id
     * @return 预签名预览地址
     */
    @GetMapping("/{fileId}/preview")
    @Operation(summary = "获取文件预览地址")
    public ApiResult<String> preview(
            @Parameter(name = "fileId", description = "文件id", required = true)
            @PathVariable("fileId") final BigInteger fileId
    ) {
        return ApiResult.success(assetFileService.preview(currentUserId(), fileId));
    }

    /**
     * 获取文件下载地址.
     *
     * @param fileId 文件id
     * @return 预签名下载地址
     */
    @GetMapping("/{fileId}/download")
    @Operation(summary = "获取文件下载地址")
    public ApiResult<String> download(
            @Parameter(name = "fileId", description = "文件id", required = true)
            @PathVariable("fileId") final BigInteger fileId
    ) {
        return ApiResult.success(assetFileService.download(currentUserId(), fileId));
    }

    /**
     * 删除文件.
     *
     * @param fileId 文件id
     * @return 通用响应
     */
    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除文件")
    public ApiResult<Void> delete(
            @Parameter(name = "fileId", description = "文件id", required = true)
            @PathVariable("fileId") final BigInteger fileId
    ) {
        assetFileService.delete(currentUserId(), fileId);
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
