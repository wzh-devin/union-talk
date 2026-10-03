package com.devin.uniontalk.file.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.file.domain.entity.AssetFile;
import com.devin.uniontalk.file.domain.entity.UploadSession;
import com.devin.uniontalk.file.mapper.AssetFileMapper;
import com.devin.uniontalk.infrastructure.file.enums.AssetFileStatusEnum;
import com.devin.uniontalk.infrastructure.file.enums.AssetFileTypeEnum;
import com.devin.uniontalk.infrastructure.file.enums.AssetOutboxEventTypeEnum;
import com.devin.uniontalk.infrastructure.file.enums.StorageTypeEnum;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 2026/06/30 18:30.
 *
 * <p>
 * 资产文件(AssetFile)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetFileDao extends ServiceImpl<AssetFileMapper, AssetFile> {

    /**
     * 查询缺少删除发件箱事件的已删除文件.
     *
     * @param batchSize 查询数量
     * @return 待补偿文件列表
     */
    public List<AssetFile> getDeletedWithoutOutboxList(final Integer batchSize) {
        return getBaseMapper().selectDeletedWithoutOutboxList(
                AssetFileStatusEnum.DELETED.name(),
                AssetOutboxEventTypeEnum.ASSET_DELETED.name(),
                batchSize
        );
    }

    /**
     * 根据上传任务创建文件.
     *
     * @param assetId     资产文件id
     * @param session     上传任务
     * @param fileType    文件类型
     * @param storageType 存储类型
     * @param storageKey  存储Key
     * @param etag        对象ETag
     * @param userId      操作用户id
     * @return 资产文件
     */
    public AssetFile createByUploadSession(
            final BigInteger assetId,
            final UploadSession session,
            final AssetFileTypeEnum fileType,
            final StorageTypeEnum storageType,
            final String storageKey,
            final String etag,
            final BigInteger userId
    ) {
        AssetFile assetFile = new AssetFile();
        assetFile.initByUploadSession(
                assetId,
                session,
                fileType.name(),
                storageType.name(),
                storageKey,
                etag,
                userId
        );
        save(assetFile);
        return assetFile;
    }

    /**
     * 查询正常文件.
     *
     * @param fileId 文件id
     * @return 资产文件
     */
    public AssetFile getNormalFileById(final BigInteger fileId) {
        return lambdaQuery()
                .eq(AssetFile::getId, fileId)
                .eq(AssetFile::getStatus, AssetFileStatusEnum.NORMAL.name())
                .one();
    }

    /**
     * 根据文件id列表批量查询正常文件.
     *
     * @param fileIdList 文件id列表
     * @return 文件列表
     */
    public List<AssetFile> getNormalFileListByIdList(final List<BigInteger> fileIdList) {
        if (Objects.isNull(fileIdList) || fileIdList.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(AssetFile::getId, fileIdList)
                .eq(AssetFile::getStatus, AssetFileStatusEnum.NORMAL.name())
                .list();
    }

    /**
     * 查询目录下文件列表.
     *
     * @param conversationId 会话id
     * @param folderId       目录id
     * @return 文件列表
     */
    public List<AssetFile> getFileListByFolderId(final BigInteger conversationId, final BigInteger folderId) {
        return lambdaQuery()
                .eq(AssetFile::getConversationId, conversationId)
                .eq(AssetFile::getFolderId, folderId)
                .eq(AssetFile::getStatus, AssetFileStatusEnum.NORMAL.name())
                .orderByDesc(AssetFile::getCreatedAt)
                .orderByDesc(AssetFile::getId)
                .list();
    }

    /**
     * 查询目录列表下文件列表.
     *
     * @param folderIdList 目录id列表
     * @return 文件列表
     */
    public List<AssetFile> getFileListByFolderIdList(final List<BigInteger> folderIdList) {
        if (Objects.isNull(folderIdList) || folderIdList.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(AssetFile::getFolderId, folderIdList)
                .eq(AssetFile::getStatus, AssetFileStatusEnum.NORMAL.name())
                .list();
    }

    /**
     * 判断同目录文件名称是否存在.
     *
     * @param conversationId 会话id
     * @param folderId       目录id
     * @param name           文件名称
     * @return true表示存在
     */
    public boolean existsNormalName(
            final BigInteger conversationId,
            final BigInteger folderId,
            final String name
    ) {
        return lambdaQuery()
                .eq(AssetFile::getConversationId, conversationId)
                .eq(AssetFile::getFolderId, folderId)
                .eq(AssetFile::getName, name)
                .eq(AssetFile::getStatus, AssetFileStatusEnum.NORMAL.name())
                .exists();
    }

    /**
     * 获取目录文件名称事务锁.
     *
     * @param conversationId 会话id
     * @param folderId       目录id
     */
    public void lockFolderName(final BigInteger conversationId, final BigInteger folderId) {
        getBaseMapper().lockFolderName(conversationId, folderId);
    }

    /**
     * 查询目录下正常文件名称集合.
     *
     * @param conversationId 会话id
     * @param folderId       目录id
     * @return 文件名称集合
     */
    public Set<String> getNormalNameSetByFolderId(
            final BigInteger conversationId,
            final BigInteger folderId
    ) {
        return lambdaQuery()
                .select(AssetFile::getName)
                .eq(AssetFile::getConversationId, conversationId)
                .eq(AssetFile::getFolderId, folderId)
                .eq(AssetFile::getStatus, AssetFileStatusEnum.NORMAL.name())
                .list()
                .stream()
                .map(AssetFile::getName)
                .collect(Collectors.toSet());
    }

    /**
     * 移动文件.
     *
     * @param assetFile      资产文件
     * @param targetFolderId 目标目录id
     * @param targetName     目标文件名称
     * @param userId         操作用户id
     */
    public void moveFile(
            final AssetFile assetFile,
            final BigInteger targetFolderId,
            final String targetName,
            final BigInteger userId
    ) {
        assetFile.moveTo(targetFolderId, targetName, userId);
        updateById(assetFile);
    }

    /**
     * 重命名文件.
     *
     * @param assetFile 资产文件
     * @param name      新文件名称
     * @param userId    操作用户id
     */
    public void renameFile(final AssetFile assetFile, final String name, final BigInteger userId) {
        assetFile.rename(name, userId);
        updateById(assetFile);
    }

    /**
     * 删除文件.
     *
     * @param assetFile 资产文件
     * @param userId    操作用户id
     */
    public void deleteFile(final AssetFile assetFile, final BigInteger userId) {
        assetFile.markDeleted(userId);
        updateById(assetFile);
    }

    /**
     * 批量删除文件.
     *
     * @param assetFileList 资产文件列表
     * @param userId        操作用户id
     */
    public void deleteFileList(final List<AssetFile> assetFileList, final BigInteger userId) {
        if (Objects.isNull(assetFileList) || assetFileList.isEmpty()) {
            return;
        }
        assetFileList.forEach(assetFile -> assetFile.markDeleted(userId));
        updateBatchById(assetFileList);
    }
}
