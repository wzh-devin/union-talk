package com.devin.uniontalk.file.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.devin.uniontalk.file.domain.vo.req.InitUploadReqVO;
import com.devin.uniontalk.file.domain.vo.resp.UploadSessionRespVO;
import com.devin.uniontalk.file.service.AssetUploadService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 2026/06/30 19:05.
 *
 * <p>
 * 资产上传任务Controller层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "upload")
@RestController
@RequiredArgsConstructor
@RequestMapping("/asset/upload")
public class AssetUploadController {

    /**
     * 资产上传任务服务.
     */
    private final AssetUploadService assetUploadService;

    /**
     * 初始化上传任务.
     *
     * @param reqVO 初始化上传任务请求参数
     * @return 上传任务响应参数
     */
    @PostMapping("/init")
    @Operation(summary = "初始化上传任务")
    public ApiResult<UploadSessionRespVO> init(@Valid @RequestBody final InitUploadReqVO reqVO) {
        return ApiResult.success(assetUploadService.init(currentUserId(), reqVO));
    }

    /**
     * 上传分片.
     *
     * @param sessionId   上传任务id
     * @param chunkIndex  分片序号
     * @param file        分片文件
     * @param chunkSha256 分片SHA256
     * @return 上传任务响应参数
     */
    @PutMapping("/{sessionId}/chunk/{chunkIndex}")
    @Operation(summary = "上传分片")
    public ApiResult<UploadSessionRespVO> uploadChunk(
            @Parameter(name = "sessionId", description = "上传任务id", required = true)
            @PathVariable("sessionId") final BigInteger sessionId,
            @Parameter(name = "chunkIndex", description = "分片序号，从0开始", required = true)
            @PathVariable("chunkIndex") final Integer chunkIndex,
            @Parameter(name = "file", description = "分片文件", required = true)
            @RequestParam("file") final MultipartFile file,
            @Parameter(name = "chunkSha256", description = "分片SHA256，可选")
            @RequestParam(value = "chunkSha256", required = false) final String chunkSha256
    ) {
        return ApiResult.success(assetUploadService.uploadChunk(
                currentUserId(),
                sessionId,
                chunkIndex,
                file,
                chunkSha256
        ));
    }

    /**
     * 完成上传任务.
     *
     * @param sessionId 上传任务id
     * @return 上传任务响应参数
     */
    @PostMapping("/{sessionId}/complete")
    @Operation(summary = "完成上传任务")
    public ApiResult<UploadSessionRespVO> complete(
            @Parameter(name = "sessionId", description = "上传任务id", required = true)
            @PathVariable("sessionId") final BigInteger sessionId
    ) {
        return ApiResult.success(assetUploadService.complete(currentUserId(), sessionId));
    }

    /**
     * 取消上传任务.
     *
     * @param sessionId 上传任务id
     * @return 通用响应
     */
    @PostMapping("/{sessionId}/cancel")
    @Operation(summary = "取消上传任务")
    public ApiResult<Void> cancel(
            @Parameter(name = "sessionId", description = "上传任务id", required = true)
            @PathVariable("sessionId") final BigInteger sessionId
    ) {
        assetUploadService.cancel(currentUserId(), sessionId);
        return ApiResult.success();
    }

    /**
     * 暂停上传任务.
     *
     * @param sessionId 上传任务id
     * @return 上传任务响应参数
     */
    @PostMapping("/{sessionId}/pause")
    @Operation(summary = "暂停上传任务")
    public ApiResult<UploadSessionRespVO> pause(
            @Parameter(name = "sessionId", description = "上传任务id", required = true)
            @PathVariable("sessionId") final BigInteger sessionId
    ) {
        return ApiResult.success(assetUploadService.pause(currentUserId(), sessionId));
    }

    /**
     * 恢复上传任务.
     *
     * @param sessionId 上传任务id
     * @return 上传任务响应参数
     */
    @PostMapping("/{sessionId}/resume")
    @Operation(summary = "恢复上传任务")
    public ApiResult<UploadSessionRespVO> resume(
            @Parameter(name = "sessionId", description = "上传任务id", required = true)
            @PathVariable("sessionId") final BigInteger sessionId
    ) {
        return ApiResult.success(assetUploadService.resume(currentUserId(), sessionId));
    }

    /**
     * 查询上传任务.
     *
     * @param sessionId 上传任务id
     * @return 上传任务响应参数
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "查询上传任务")
    public ApiResult<UploadSessionRespVO> getDetail(
            @Parameter(name = "sessionId", description = "上传任务id", required = true)
            @PathVariable("sessionId") final BigInteger sessionId
    ) {
        return ApiResult.success(assetUploadService.getDetail(currentUserId(), sessionId));
    }

    /**
     * 删除映射，兼容客户端使用 DELETE 取消任务.
     *
     * @param sessionId 上传任务id
     * @return 通用响应
     */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "取消上传任务")
    public ApiResult<Void> delete(
            @Parameter(name = "sessionId", description = "上传任务id", required = true)
            @PathVariable("sessionId") final BigInteger sessionId
    ) {
        assetUploadService.cancel(currentUserId(), sessionId);
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
