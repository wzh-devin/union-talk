package com.devin.uniontalk.message.grpc.client;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.exception.BizException;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.grpc.file.domain.request.BatchGetAssetFileInfoRequest;
import com.devin.uniontalk.grpc.file.domain.response.BatchGetAssetFileInfoResponse;
import com.devin.uniontalk.grpc.file.service.FileGrpcServiceGrpc;
import com.devin.uniontalk.message.domain.vo.resp.MessageAssetInfoRespVO;
import com.devin.uniontalk.message.grpc.convertor.MessageGrpcConvertor;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * 2026/07/22 14:12.
 *
 * <p>
 * 文件资产信息Grpc客户端
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
public class FileAssetInfoGrpcClient {

    /**
     * 文件Grpc阻塞调用客户端.
     */
    @GrpcClient("union-talk-file")
    private FileGrpcServiceGrpc.FileGrpcServiceBlockingStub fileGrpcServiceBlockingStub;

    /**
     * 批量查询资产文件信息映射.
     *
     * @param assetIdList 资产文件id列表
     * @return 资产文件信息映射
     */
    public Map<BigInteger, MessageAssetInfoRespVO> getAssetInfoMap(final List<BigInteger> assetIdList) {
        if (Objects.isNull(assetIdList) || assetIdList.isEmpty()) {
            return Map.of();
        }
        try {
            List<BigInteger> distinctAssetIdList = assetIdList.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (distinctAssetIdList.isEmpty()) {
                return Map.of();
            }
            BatchGetAssetFileInfoResponse response = fileGrpcServiceBlockingStub.batchGetAssetFileInfo(
                    BatchGetAssetFileInfoRequest.newBuilder()
                            .addAllAssetId(distinctAssetIdList.stream().map(BigInteger::toString).toList())
                            .build()
            );
            AssertUtils.isFalse(
                    Objects.isNull(response) || !response.getBr().getSuccess(),
                    BizErrorEnum.MESSAGE_ASSET_QUERY_FAILED
            );
            return response.getAssetFileInfoList()
                    .stream()
                    .collect(Collectors.toMap(
                            assetFileInfo -> new BigInteger(assetFileInfo.getAssetId()),
                            MessageGrpcConvertor.INSTANCE::toAssetInfoRespVO,
                            (oldValue, newValue) -> oldValue
                    ));
        } catch (Exception e) {
            log.error("调用文件服务批量查询资产信息Grpc失败, assetIdList={}", assetIdList, e);
            throw new BizException(BizErrorEnum.MESSAGE_ASSET_QUERY_FAILED);
        }
    }
}
