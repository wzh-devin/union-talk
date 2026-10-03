package com.devin.uniontalk.user.grpc.client;

import cn.hutool.core.io.FileUtil;
import com.devin.uniontalk.base.constant.StorageKeyConstant;
import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.exception.BizException;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.grpc.file.domain.request.UploadFileRequest;
import com.devin.uniontalk.grpc.file.domain.request.UploadFileResponse;
import com.devin.uniontalk.grpc.file.service.FileGrpcServiceGrpc;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigInteger;

/**
 * 2026/7/16 18:23.
 *
 * <p>
 * 文件客户端
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
public class FileGrpcClient {

    /**
     * 文件服务阻塞式 gRPC 客户端.
     */
    @GrpcClient("union-talk-file")
    private FileGrpcServiceGrpc.FileGrpcServiceBlockingStub fileGrpcServiceBlockingStub;

    /**
     * 上传文件.
     *
     * @param userId 用户ID
     * @param file   文件
     * @return 文件地址
     */
    public String uploadFile(final BigInteger userId, final MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String suffix = FileUtil.getSuffix(file.getOriginalFilename());
            UploadFileResponse uploadFileResponse = fileGrpcServiceBlockingStub.uploadFile(
                    UploadFileRequest.newBuilder()
                            .setFile(ByteString.copyFrom(bytes))
                            .setStorageKey(StorageKeyConstant.buildStorageKey(
                                    StorageKeyConstant.USER_AVATAR,
                                    userId.toString(),
                                    suffix
                            ))
                            .build()
            );
            AssertUtils.isTrue(uploadFileResponse.getBr().getSuccess(), BizErrorEnum.UPLOAD_ERROR);
            return uploadFileResponse.getFileUrl();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BizException(BizErrorEnum.UPLOAD_ERROR);
        }
    }
}
