package com.devin.uniontalk.file.service;

import com.devin.uniontalk.file.domain.vo.resp.AssetFileRespVO;
import com.devin.uniontalk.file.domain.model.AgentResourceContent;
import java.math.BigInteger;
import java.util.List;

/**
 * 2026/06/30 15:20:58.
 *
 * <p>
 *  资产文件(AssetFile)Service层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface AssetFileService {

    /**
     * 查询目录下文件列表.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @param folderId       文件夹id
     * @return 文件列表
     */
    List<AssetFileRespVO> getFileList(
            BigInteger userId,
            BigInteger conversationId,
            BigInteger folderId
    );

    /**
     * 根据文件id列表批量查询文件.
     *
     * @param fileIdList 文件id列表
     * @return 文件列表
     */
    List<AssetFileRespVO> getFileListByIdList(List<BigInteger> fileIdList);

    /**
     * 查询Agent索引使用的资源快照.
     *
     * @param fileId         文件id
     * @param resourceVersion 内容版本
     * @return Agent索引资源快照
     */
    AgentResourceContent getAgentResourceContent(BigInteger fileId, Integer resourceVersion);

    /**
     * 移动文件.
     *
     * @param userId         用户id
     * @param fileId         文件id
     * @param targetFolderId 目标文件夹id
     * @return 文件响应参数
     */
    AssetFileRespVO move(
            BigInteger userId,
            BigInteger fileId,
            BigInteger targetFolderId
    );

    /**
     * 重命名文件.
     *
     * @param userId 用户id
     * @param fileId 文件id
     * @param name   文件名称
     * @return 文件响应参数
     */
    AssetFileRespVO rename(
            BigInteger userId,
            BigInteger fileId,
            String name
    );

    /**
     * 获取文件预览地址.
     *
     * @param userId 用户id
     * @param fileId 文件id
     * @return 预签名预览地址
     */
    String preview(BigInteger userId, BigInteger fileId);

    /**
     * 获取文件下载地址.
     *
     * @param userId 用户id
     * @param fileId 文件id
     * @return 预签名下载地址
     */
    String download(BigInteger userId, BigInteger fileId);

    /**
     * 删除文件.
     *
     * @param userId 用户id
     * @param fileId 文件id
     */
    void delete(BigInteger userId, BigInteger fileId);
}
