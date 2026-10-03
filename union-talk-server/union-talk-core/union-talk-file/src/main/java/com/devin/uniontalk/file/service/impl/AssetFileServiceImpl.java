package com.devin.uniontalk.file.service.impl;

import com.devin.uniontalk.base.constant.SystemConstant;
import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.common.storage.service.ObjectStorageService;
import com.devin.uniontalk.file.dao.AssetFileDao;
import com.devin.uniontalk.file.dao.AssetFolderDao;
import com.devin.uniontalk.file.dao.AssetOutboxDao;
import com.devin.uniontalk.file.domain.entity.AssetFile;
import com.devin.uniontalk.file.domain.entity.AssetFolder;
import com.devin.uniontalk.file.domain.entity.convertor.AssetFileConvertor;
import com.devin.uniontalk.file.domain.model.AgentResourceContent;
import com.devin.uniontalk.file.domain.model.AssetFileAccessContext;
import com.devin.uniontalk.file.domain.model.AssetNameGenerator;
import com.devin.uniontalk.file.domain.model.ConversationAssetAccess;
import com.devin.uniontalk.file.domain.vo.resp.AssetFileRespVO;
import com.devin.uniontalk.file.grpc.client.AssetAccessGrpcClient;
import com.devin.uniontalk.file.service.AssetFileService;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2026/06/30 18:55.
 *
 * <p>
 * 资产文件(AssetFile)ServiceImpl层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetFileServiceImpl implements AssetFileService {

    /**
     * 资产文件 Dao.
     */
    private final AssetFileDao assetFileDao;

    /**
     * 资产目录 Dao.
     */
    private final AssetFolderDao assetFolderDao;

    /**
     * 对象存储服务.
     */
    private final ObjectStorageService objectStorageService;

    /**
     * 会话资产访问上下文 Grpc 客户端.
     */
    private final AssetAccessGrpcClient assetAccessGrpcClient;

    /**
     * 资产事件发件箱 Dao.
     */
    private final AssetOutboxDao assetOutboxDao;

    /**
     * 查询目录下文件列表.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @param folderId       文件夹id
     * @return 文件列表
     */
    @Override
    public List<AssetFileRespVO> getFileList(
            final BigInteger userId,
            final BigInteger conversationId,
            final BigInteger folderId
    ) {
        ConversationAssetAccess access = assetAccessGrpcClient.getAccess(conversationId, userId);
        AssetFolder folder = assetFolderDao.getFolderOrRoot(conversationId, folderId);
        AssertUtils.nonNull(folder, BizErrorEnum.ASSET_FOLDER_NOT_FOUND);
        return AssetFileConvertor.INSTANCE.toRespVOList(
                assetFileDao.getFileListByFolderId(conversationId, folder.getId()),
                access,
                folder
        );
    }

    /**
     * 根据文件id列表批量查询文件.
     *
     * @param fileIdList 文件id列表
     * @return 文件列表
     */
    @Override
    public List<AssetFileRespVO> getFileListByIdList(final List<BigInteger> fileIdList) {
        return AssetFileConvertor.INSTANCE.toRespVOList(assetFileDao.getNormalFileListByIdList(fileIdList));
    }

    /**
     * 查询Agent索引使用的资源快照.
     *
     * @param fileId          文件id
     * @param resourceVersion 内容版本
     * @return Agent索引资源快照
     */
    @Override
    public AgentResourceContent getAgentResourceContent(
            final BigInteger fileId,
            final Integer resourceVersion
    ) {
        AssetFile assetFile = assetFileDao.getNormalFileById(fileId);
        AssertUtils.nonNull(assetFile, BizErrorEnum.ASSET_FILE_NOT_FOUND);
        AssertUtils.isTrue(
                Objects.equals(assetFile.getResourceVersion(), resourceVersion),
                BizErrorEnum.ASSET_FILE_NOT_FOUND
        );
        String downloadUrl = objectStorageService.getPresignedObjectUrl(
                assetFile.getBucketName(),
                assetFile.getStorageKey(),
                assetFile.getMimeType(),
                "attachment",
                SystemConstant.FILE_ACCESS_URL_EXPIRY_SECONDS
        );
        return new AgentResourceContent(
                assetFile.getId(),
                assetFile.getConversationId(),
                assetFile.getResourceVersion(),
                assetFile.getName(),
                assetFile.getMimeType(),
                assetFile.getSha256(),
                assetFile.getEtag(),
                assetFile.getFolderId(),
                "",
                downloadUrl
        );
    }

    /**
     * 移动文件.
     *
     * @param userId         用户id
     * @param fileId         文件id
     * @param targetFolderId 目标文件夹id
     * @return 文件响应参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetFileRespVO move(
            final BigInteger userId,
            final BigInteger fileId,
            final BigInteger targetFolderId
    ) {
        AssetFileAccessContext fileAccessContext = getFileAccessContext(fileId, userId);
        AssetFile assetFile = fileAccessContext.assetFile();
        ConversationAssetAccess access = fileAccessContext.access();
        AssertUtils.isTrue(fileAccessContext.canWriteContent(), BizErrorEnum.ASSET_PERMISSION_DENIED);
        AssetFolder targetFolder = assetFolderDao.getFolderOrRoot(
                assetFile.getConversationId(),
                targetFolderId
        );
        AssertUtils.nonNull(targetFolder, BizErrorEnum.ASSET_FOLDER_NOT_FOUND);
        AssertUtils.isFalse(targetFolder.isMemberRoot(), BizErrorEnum.ASSET_TARGET_FOLDER_INVALID);
        AssertUtils.isTrue(targetFolder.canAcceptMove(access), BizErrorEnum.ASSET_PERMISSION_DENIED);
        BigInteger realTargetFolderId = targetFolder.getId();
        String targetName = assetFile.getName();
        if (!Objects.equals(assetFile.getFolderId(), realTargetFolderId)) {
            assetFileDao.lockFolderName(assetFile.getConversationId(), realTargetFolderId);
            targetName = AssetNameGenerator.getAvailableFileName(
                    assetFile.getName(),
                    assetFileDao.getNormalNameSetByFolderId(
                            assetFile.getConversationId(),
                            realTargetFolderId
                    )
            );
        }
        assetFileDao.moveFile(assetFile, realTargetFolderId, targetName, userId);
        return AssetFileConvertor.INSTANCE.toRespVO(assetFile, access, targetFolder);
    }

    /**
     * 重命名文件.
     *
     * @param userId 用户id
     * @param fileId 文件id
     * @param name   文件名称
     * @return 文件响应参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetFileRespVO rename(final BigInteger userId, final BigInteger fileId, final String name) {
        AssetFileAccessContext fileAccessContext = getFileAccessContext(fileId, userId);
        AssertUtils.isTrue(fileAccessContext.canWriteContent(), BizErrorEnum.ASSET_PERMISSION_DENIED);
        AssetFile assetFile = fileAccessContext.assetFile();
        if (!Objects.equals(assetFile.getName(), name)) {
            assetFileDao.lockFolderName(assetFile.getConversationId(), assetFile.getFolderId());
            AssertUtils.isFalse(
                    assetFileDao.existsNormalName(assetFile.getConversationId(), assetFile.getFolderId(), name),
                    BizErrorEnum.ASSET_NAME_DUPLICATE
            );
        }
        assetFileDao.renameFile(assetFile, name, userId);
        return AssetFileConvertor.INSTANCE.toRespVO(
                assetFile,
                fileAccessContext.access(),
                fileAccessContext.sourceFolder()
        );
    }

    /**
     * 获取文件预览地址.
     *
     * @param userId 用户id
     * @param fileId 文件id
     * @return 预签名预览地址
     */
    @Override
    public String preview(final BigInteger userId, final BigInteger fileId) {
        AssetFile assetFile = getFileAccessContext(fileId, userId).assetFile();
        return objectStorageService.getPresignedObjectUrl(
                assetFile.getBucketName(),
                assetFile.getStorageKey(),
                assetFile.getPreviewMimeType(),
                "inline",
                SystemConstant.FILE_ACCESS_URL_EXPIRY_SECONDS
        );
    }

    /**
     * 获取文件下载地址.
     *
     * @param userId 用户id
     * @param fileId 文件id
     * @return 预签名下载地址
     */
    @Override
    public String download(final BigInteger userId, final BigInteger fileId) {
        AssetFile assetFile = getFileAccessContext(fileId, userId).assetFile();
        String encodedFileName = URLEncoder.encode(assetFile.getName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return objectStorageService.getPresignedObjectUrl(
                assetFile.getBucketName(),
                assetFile.getStorageKey(),
                assetFile.getPreviewMimeType(),
                "attachment; filename*=UTF-8''" + encodedFileName,
                SystemConstant.FILE_ACCESS_URL_EXPIRY_SECONDS
        );
    }

    /**
     * 删除文件.
     *
     * @param userId 用户id
     * @param fileId 文件id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(final BigInteger userId, final BigInteger fileId) {
        AssetFileAccessContext fileAccessContext = getFileAccessContext(fileId, userId);
        AssertUtils.isTrue(fileAccessContext.canWriteContent(), BizErrorEnum.ASSET_PERMISSION_DENIED);
        AssetFile assetFile = fileAccessContext.assetFile();
        assetFileDao.deleteFile(assetFile, userId);
        assetOutboxDao.createDeletedEvent(assetFile, new Date());
        objectStorageService.removeObject(assetFile.getBucketName(), assetFile.getStorageKey());
    }

    /**
     * 查询资产文件访问上下文.
     *
     * @param fileId 文件id
     * @param userId 用户id
     * @return 资产文件访问上下文
     */
    private AssetFileAccessContext getFileAccessContext(
            final BigInteger fileId,
            final BigInteger userId
    ) {
        AssetFile assetFile = assetFileDao.getNormalFileById(fileId);
        AssertUtils.nonNull(assetFile, BizErrorEnum.ASSET_FILE_NOT_FOUND);
        ConversationAssetAccess access = assetAccessGrpcClient.getAccess(assetFile.getConversationId(), userId);
        AssetFolder sourceFolder = assetFolderDao.getFolderOrRoot(
                assetFile.getConversationId(),
                assetFile.getFolderId()
        );
        AssertUtils.nonNull(sourceFolder, BizErrorEnum.ASSET_FOLDER_NOT_FOUND);
        return new AssetFileAccessContext(assetFile, sourceFolder, access);
    }
}
