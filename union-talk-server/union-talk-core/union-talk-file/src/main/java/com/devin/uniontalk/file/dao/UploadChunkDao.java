package com.devin.uniontalk.file.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.file.domain.entity.UploadChunk;
import com.devin.uniontalk.file.mapper.UploadChunkMapper;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 2026/06/30 18:30.
 *
 * <p>
 * 上传分片表(UploadChunk)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadChunkDao extends ServiceImpl<UploadChunkMapper, UploadChunk> {

    /**
     * 查询任务分片.
     *
     * @param sessionId  任务id
     * @param chunkIndex 分片序号
     * @return 分片
     */
    public UploadChunk getBySessionIdAndChunkIndex(final BigInteger sessionId, final Integer chunkIndex) {
        return lambdaQuery()
                .eq(UploadChunk::getSessionId, sessionId)
                .eq(UploadChunk::getChunkIndex, chunkIndex)
                .one();
    }

    /**
     * 创建分片记录.
     *
     * @param sessionId   任务id
     * @param chunkIndex  分片序号
     * @param chunkSize   分片大小
     * @param chunkSha256 分片SHA256
     * @param storageKey  分片对象Key
     * @param etag        对象ETag
     * @return 分片记录
     */
    public UploadChunk createUploadChunk(
            final BigInteger sessionId,
            final Integer chunkIndex,
            final BigInteger chunkSize,
            final String chunkSha256,
            final String storageKey,
            final String etag
    ) {
        UploadChunk uploadChunk = new UploadChunk();
        uploadChunk.initChunk(
                IdGenerator.nextIdBigInteger(),
                sessionId,
                chunkIndex,
                chunkSize,
                chunkSha256,
                storageKey,
                etag
        );
        save(uploadChunk);
        return uploadChunk;
    }

    /**
     * 查询任务分片列表.
     *
     * @param sessionId 任务id
     * @return 分片列表
     */
    public List<UploadChunk> getChunkListBySessionId(final BigInteger sessionId) {
        return lambdaQuery()
                .eq(UploadChunk::getSessionId, sessionId)
                .orderByAsc(UploadChunk::getChunkIndex)
                .list();
    }

    /**
     * 查询任务分片对象Key列表.
     *
     * @param sessionId 任务id
     * @return 分片对象Key列表
     */
    public List<String> getChunkStorageKeyList(final BigInteger sessionId) {
        return getChunkListBySessionId(sessionId)
                .stream()
                .map(UploadChunk::getStorageKey)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 删除任务分片记录.
     *
     * @param sessionId 任务id
     */
    public void deleteBySessionId(final BigInteger sessionId) {
        lambdaUpdate()
                .eq(UploadChunk::getSessionId, sessionId)
                .remove();
    }
}
