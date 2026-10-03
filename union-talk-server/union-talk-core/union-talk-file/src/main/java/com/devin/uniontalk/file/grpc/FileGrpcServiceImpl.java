package com.devin.uniontalk.file.grpc;

import cn.hutool.http.ContentType;
import com.devin.uniontalk.common.storage.domain.model.StorageObjectWriteResult;
import com.devin.uniontalk.common.storage.properties.StorageProperties;
import com.devin.uniontalk.common.storage.service.ObjectStorageService;
import com.devin.uniontalk.file.service.AssetFileService;
import com.devin.uniontalk.file.service.AvatarThumbnailService;
import com.devin.uniontalk.grpc.base.BaseResponse;
import com.devin.uniontalk.grpc.file.domain.model.AgentResourceContent;
import com.devin.uniontalk.grpc.file.domain.model.AssetFileInfo;
import com.devin.uniontalk.grpc.file.domain.request.BatchGetAssetFileInfoRequest;
import com.devin.uniontalk.grpc.file.domain.request.GetAgentResourceContentRequest;
import com.devin.uniontalk.grpc.file.domain.request.UploadFileRequest;
import com.devin.uniontalk.grpc.file.domain.request.UploadFileResponse;
import com.devin.uniontalk.grpc.file.domain.response.BatchGetAssetFileInfoResponse;
import com.devin.uniontalk.grpc.file.domain.response.GetAgentResourceContentResponse;
import com.devin.uniontalk.grpc.file.service.FileGrpcServiceGrpc;
import com.devin.uniontalk.web.response.ResultEnum;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.util.StringUtils;

/**
 * 2026/7/16 18:27.
 *
 * <p>
 * 文件grpc服务实现类
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class FileGrpcServiceImpl extends FileGrpcServiceGrpc.FileGrpcServiceImplBase {

    /**
     * 缩略图文件名标识.
     */
    private static final String THUMBNAIL_FILE_MARK = "_64x64.webp";

    /**
     * WebP内容类型.
     */
    private static final String WEBP_CONTENT_TYPE = "image/webp";

    /**
     * 对象存储服务.
     */
    private final ObjectStorageService objectStorageService;

    /**
     * 对象存储配置.
     */
    private final StorageProperties storageProperties;

    /**
     * 头像缩略图服务.
     */
    private final AvatarThumbnailService avatarThumbnailService;

    /**
     * 资产文件服务.
     */
    private final AssetFileService assetFileService;

    /**
     * 上传原图和头像缩略图.
     *
     * @param request 上传文件请求
     * @param responseObserver 上传文件响应观察器
     */
    @Override
    public void uploadFile(
            final UploadFileRequest request,
            final StreamObserver<UploadFileResponse> responseObserver
    ) {
        ByteString fileBytes = request.getFile();
        String originalStorageKey = request.getStorageKey();
        String thumbnailStorageKey = buildThumbnailStorageKey(originalStorageKey);
        boolean originalUploaded = false;
        StorageObjectWriteResult thumbnailResult;

        try {
            byte[] thumbnailBytes;
            try (InputStream thumbnailSourceInputStream = fileBytes.newInput()) {
                thumbnailBytes = avatarThumbnailService.createThumbnail(thumbnailSourceInputStream);
            }

            try (InputStream originalInputStream = fileBytes.newInput()) {
                objectStorageService.putObject(
                        storageProperties.getBucketName(),
                        originalStorageKey,
                        originalInputStream,
                        fileBytes.size(),
                        ContentType.OCTET_STREAM.getValue()
                );
                originalUploaded = true;
            }

            try (InputStream thumbnailInputStream = new ByteArrayInputStream(thumbnailBytes)) {
                thumbnailResult = objectStorageService.putObject(
                        storageProperties.getBucketName(),
                        thumbnailStorageKey,
                        thumbnailInputStream,
                        thumbnailBytes.length,
                        WEBP_CONTENT_TYPE
                );
            }
        } catch (Exception e) {
            if (originalUploaded) {
                objectStorageService.removeObject(storageProperties.getBucketName(), originalStorageKey);
            }
            log.error("文件上传失败, storageKey={}", originalStorageKey, e);
            responseObserver.onNext(failureResponse(
                    ResultEnum.SYSTEM_ERROR.getCode(),
                    ResultEnum.SYSTEM_ERROR.getMessage()
            ));
            responseObserver.onCompleted();
            return;
        }

        String thumbnailFileUrl = storageProperties.getExportUrl()
                + "/" + thumbnailResult.getBucketName()
                + "/" + thumbnailResult.getStorageKey();
        responseObserver.onNext(successResponse(thumbnailFileUrl));
        responseObserver.onCompleted();
    }

    /**
     * 批量查询资产文件信息.
     *
     * @param request          批量查询资产文件信息请求
     * @param responseObserver 批量查询资产文件信息响应观察器
     */
    @Override
    public void batchGetAssetFileInfo(
            final BatchGetAssetFileInfoRequest request,
            final StreamObserver<BatchGetAssetFileInfoResponse> responseObserver
    ) {
        try {
            List<BigInteger> assetIdList = request.getAssetIdList()
                    .stream()
                    .filter(StringUtils::hasText)
                    .map(BigInteger::new)
                    .distinct()
                    .toList();
            List<AssetFileInfo> assetFileInfoList = assetFileService.getFileListByIdList(assetIdList)
                    .stream()
                    .map(assetFile -> AssetFileInfo.newBuilder()
                            .setAssetId(assetFile.getId().toString())
                            .setConversationId(assetFile.getConversationId().toString())
                            .setName(Objects.toString(assetFile.getName(), ""))
                            .setFileExt(Objects.toString(assetFile.getFileExt(), ""))
                            .setFileType(Objects.toString(assetFile.getFileType(), ""))
                            .setFileSize(assetFile.getFileSize().toString())
                            .setMimeType(Objects.toString(assetFile.getMimeType(), ""))
                            .build())
                    .toList();
            responseObserver.onNext(BatchGetAssetFileInfoResponse.newBuilder()
                    .setBr(successBaseResponse())
                    .addAllAssetFileInfo(assetFileInfoList)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("批量查询资产文件信息Grpc失败, assetIdList={}", request.getAssetIdList(), e);
            responseObserver.onNext(BatchGetAssetFileInfoResponse.newBuilder()
                    .setBr(BaseResponse.newBuilder()
                            .setSuccess(Boolean.FALSE)
                            .setCode(ResultEnum.SYSTEM_ERROR.getCode())
                            .setMessage(Objects.toString(e.getMessage(), ""))
                            .build())
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * 查询Agent索引资源快照和临时下载地址.
     *
     * @param request          Agent资源内容请求
     * @param responseObserver Agent资源内容响应观察器
     */
    @Override
    public void getAgentResourceContent(
            final GetAgentResourceContentRequest request,
            final StreamObserver<GetAgentResourceContentResponse> responseObserver
    ) {
        try {
            com.devin.uniontalk.file.domain.model.AgentResourceContent resource =
                    assetFileService.getAgentResourceContent(
                            new BigInteger(request.getAssetId()),
                            request.getResourceVersion()
                    );
            AgentResourceContent.Builder contentBuilder = AgentResourceContent.newBuilder()
                    .setAssetId(resource.assetId().toString())
                    .setConversationId(resource.conversationId().toString())
                    .setResourceVersion(resource.resourceVersion())
                    .setFileName(Objects.toString(resource.fileName(), ""))
                    .setMimeType(Objects.toString(resource.mimeType(), ""))
                    .setSha256(Objects.toString(resource.sha256(), ""))
                    .setEtag(Objects.toString(resource.etag(), ""))
                    .setPathText(Objects.toString(resource.pathText(), ""))
                    .setDownloadUrl(Objects.toString(resource.downloadUrl(), ""));
            if (Objects.nonNull(resource.folderId())) {
                contentBuilder.setFolderId(resource.folderId().toString());
            }
            responseObserver.onNext(GetAgentResourceContentResponse.newBuilder()
                    .setBr(successBaseResponse())
                    .setResource(contentBuilder.build())
                    .build());
        } catch (Exception e) {
            log.error("查询Agent索引资源失败, assetId={}", request.getAssetId(), e);
            responseObserver.onNext(GetAgentResourceContentResponse.newBuilder()
                    .setBr(BaseResponse.newBuilder()
                            .setSuccess(Boolean.FALSE)
                            .setCode(ResultEnum.SYSTEM_ERROR.getCode())
                            .setMessage(Objects.toString(e.getMessage(), ""))
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    /**
     * 构建头像缩略图存储Key.
     *
     * @param originalStorageKey 原图存储Key
     * @return 缩略图存储Key
     */
    private String buildThumbnailStorageKey(final String originalStorageKey) {
        int pathSeparatorIndex = originalStorageKey.lastIndexOf('/');
        int extensionSeparatorIndex = originalStorageKey.lastIndexOf('.');
        if (extensionSeparatorIndex <= pathSeparatorIndex) {
            return originalStorageKey + THUMBNAIL_FILE_MARK;
        }
        return originalStorageKey.substring(0, extensionSeparatorIndex) + THUMBNAIL_FILE_MARK;
    }

    /**
     * 构建失败响应.
     *
     * @param code    状态码
     * @param message 状态信息
     * @return 文件上传响应
     */
    private UploadFileResponse failureResponse(final Integer code, final String message) {
        return UploadFileResponse.newBuilder()
                .setBr(BaseResponse.newBuilder()
                        .setSuccess(Boolean.FALSE)
                        .setCode(code)
                        .setMessage(message)
                        .build())
                .build();
    }

    /**
     * 构建成功响应.
     *
     * @param fileUrl 文件Url
     * @return 文件上传响应
     */
    private UploadFileResponse successResponse(final String fileUrl) {
        return UploadFileResponse.newBuilder()
                .setBr(successBaseResponse())
                .setFileUrl(fileUrl)
                .build();
    }

    /**
     * 构建成功基础响应.
     *
     * @return 基础响应
     */
    private BaseResponse successBaseResponse() {
        return BaseResponse.newBuilder()
                .setSuccess(Boolean.TRUE)
                .setCode(ResultEnum.SUCCESS.getCode())
                .setMessage(ResultEnum.SUCCESS.getMessage())
                .build();
    }
}
