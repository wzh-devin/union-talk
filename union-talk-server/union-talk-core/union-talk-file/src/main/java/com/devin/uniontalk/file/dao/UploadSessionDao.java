package com.devin.uniontalk.file.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.file.domain.entity.UploadSession;
import com.devin.uniontalk.file.mapper.UploadSessionMapper;
import java.math.BigInteger;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 2026/06/30 18:30.
 *
 * <p>
 * 上传任务表(UploadSession)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadSessionDao extends ServiceImpl<UploadSessionMapper, UploadSession> {

    /**
     * 创建上传任务.
     *
     * @param conversationId 会话id
     * @param folderId       文件夹id
     * @param fileName       文件名称
     * @param fileExt        文件扩展名
     * @param mimeType       MIME 类型
     * @param fileSize       文件大小
     * @param fileSha256     文件摘要
     * @param chunkSize      分片大小
     * @param chunkCount     分片数量
     * @param bucketName     存储桶名称
     * @param userId         操作用户id
     * @return 上传任务
     */
    public UploadSession createUploadSession(
            final BigInteger conversationId,
            final BigInteger folderId,
            final String fileName,
            final String fileExt,
            final String mimeType,
            final BigInteger fileSize,
            final String fileSha256,
            final BigInteger chunkSize,
            final Integer chunkCount,
            final String bucketName,
            final BigInteger userId
    ) {
        UploadSession session = new UploadSession();
        session.initSession(
                IdGenerator.nextIdBigInteger(),
                IdGenerator.nextKey(),
                conversationId,
                folderId,
                fileName,
                fileExt,
                mimeType,
                fileSize,
                fileSha256,
                chunkSize,
                chunkCount,
                bucketName,
                userId
        );
        save(session);
        return session;
    }

    /**
     * 查询用户上传任务.
     *
     * @param sessionId 任务id
     * @param userId    用户id
     * @return 上传任务
     */
    public UploadSession getByIdAndUserId(final BigInteger sessionId, final BigInteger userId) {
        return lambdaQuery()
                .eq(UploadSession::getId, sessionId)
                .eq(UploadSession::getCreatedBy, userId)
                .one();
    }

    /**
     * 加锁查询用户上传任务.
     *
     * @param sessionId 任务id
     * @param userId    用户id
     * @return 上传任务
     */
    public UploadSession getByIdAndUserIdForUpdate(final BigInteger sessionId, final BigInteger userId) {
        return lambdaQuery()
                .eq(UploadSession::getId, sessionId)
                .eq(UploadSession::getCreatedBy, userId)
                .last("FOR UPDATE")
                .one();
    }

    /**
     * 原子增加已上传分片数量.
     *
     * @param sessionId 上传任务id
     */
    public void increaseUploadedChunkCount(final BigInteger sessionId) {
        lambdaUpdate()
                .setSql("uploaded_chunk_count = uploaded_chunk_count + 1")
                .set(UploadSession::getUpdatedAt, new Date())
                .eq(UploadSession::getId, sessionId)
                .update();
    }

    /**
     * 更新任务.
     *
     * @param session 上传任务
     */
    public void updateSession(final UploadSession session) {
        updateById(session);
    }
}
