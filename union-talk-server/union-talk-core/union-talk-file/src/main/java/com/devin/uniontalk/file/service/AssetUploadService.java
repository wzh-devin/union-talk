package com.devin.uniontalk.file.service;

import com.devin.uniontalk.file.domain.vo.req.InitUploadReqVO;
import com.devin.uniontalk.file.domain.vo.resp.UploadSessionRespVO;
import java.math.BigInteger;
import org.springframework.web.multipart.MultipartFile;

/**
 * 2026/06/30 18:50.
 *
 * <p>
 * 资产上传任务Service层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface AssetUploadService {

    /**
     * 初始化上传任务.
     *
     * @param userId 用户id
     * @param reqVO  初始化上传任务请求参数
     * @return 上传任务响应参数
     */
    UploadSessionRespVO init(BigInteger userId, InitUploadReqVO reqVO);

    /**
     * 上传分片.
     *
     * @param userId       用户id
     * @param sessionId    上传任务id
     * @param chunkIndex   分片序号
     * @param file         分片文件
     * @param chunkSha256  分片SHA256
     * @return 上传任务响应参数
     */
    UploadSessionRespVO uploadChunk(
            BigInteger userId,
            BigInteger sessionId,
            Integer chunkIndex,
            MultipartFile file,
            String chunkSha256
    );

    /**
     * 完成上传任务.
     *
     * @param userId    用户id
     * @param sessionId 上传任务id
     * @return 上传任务响应参数
     */
    UploadSessionRespVO complete(BigInteger userId, BigInteger sessionId);

    /**
     * 取消上传任务.
     *
     * @param userId    用户id
     * @param sessionId 上传任务id
     */
    void cancel(BigInteger userId, BigInteger sessionId);

    /**
     * 暂停上传任务.
     *
     * @param userId    用户id
     * @param sessionId 上传任务id
     * @return 上传任务响应参数
     */
    UploadSessionRespVO pause(BigInteger userId, BigInteger sessionId);

    /**
     * 恢复上传任务.
     *
     * @param userId    用户id
     * @param sessionId 上传任务id
     * @return 上传任务响应参数
     */
    UploadSessionRespVO resume(BigInteger userId, BigInteger sessionId);

    /**
     * 查询上传任务.
     *
     * @param userId    用户id
     * @param sessionId 上传任务id
     * @return 上传任务响应参数
     */
    UploadSessionRespVO getDetail(BigInteger userId, BigInteger sessionId);
}
