package com.devin.uniontalk.file.service;

import com.devin.uniontalk.file.domain.vo.resp.AssetFolderTreeRespVO;
import java.math.BigInteger;

/**
 * 2026/06/30 15:20:59.
 *
 * <p>
 *  资产目录(AssetFolder)Service层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface AssetFolderService {

    /**
     * 创建目录.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @param parentId       父级目录id
     * @param name           目录名称
     * @return 目录树响应参数
     */
    AssetFolderTreeRespVO create(
            BigInteger userId,
            BigInteger conversationId,
            BigInteger parentId,
            String name
    );

    /**
     * 查询目录树.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @return 目录树响应参数
     */
    AssetFolderTreeRespVO getTree(
            BigInteger userId,
            BigInteger conversationId
    );

    /**
     * 移动目录.
     *
     * @param userId         用户id
     * @param folderId       目录id
     * @param targetParentId 目标父级目录id
     * @return 目录树响应参数
     */
    AssetFolderTreeRespVO move(
            BigInteger userId,
            BigInteger folderId,
            BigInteger targetParentId
    );

    /**
     * 重命名目录.
     *
     * @param userId   用户id
     * @param folderId 目录id
     * @param name     目录名称
     * @return 目录树响应参数
     */
    AssetFolderTreeRespVO rename(
            BigInteger userId,
            BigInteger folderId,
            String name
    );

    /**
     * 删除目录.
     *
     * @param userId   用户id
     * @param folderId 目录id
     */
    void delete(BigInteger userId, BigInteger folderId);
}
