package com.devin.uniontalk.file.service.impl;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.common.storage.service.ObjectStorageService;
import com.devin.uniontalk.file.dao.AssetFileDao;
import com.devin.uniontalk.file.dao.AssetFolderDao;
import com.devin.uniontalk.file.dao.AssetOutboxDao;
import com.devin.uniontalk.file.domain.entity.AssetFile;
import com.devin.uniontalk.file.domain.entity.AssetFolder;
import com.devin.uniontalk.file.domain.model.AssetNameGenerator;
import com.devin.uniontalk.file.domain.model.ConversationAssetAccess;
import com.devin.uniontalk.file.domain.vo.resp.AssetFolderPermissionRespVO;
import com.devin.uniontalk.file.domain.vo.resp.AssetFolderTreeRespVO;
import com.devin.uniontalk.file.grpc.client.AssetAccessGrpcClient;
import com.devin.uniontalk.file.service.AssetFolderService;
import com.devin.uniontalk.infrastructure.file.constant.AssetFolderConstant;
import com.devin.uniontalk.infrastructure.file.enums.AssetFolderTypeEnum;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2026/06/30 18:55.
 *
 * <p>
 * 资产目录(AssetFolder)ServiceImpl层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetFolderServiceImpl implements AssetFolderService {

    /**
     * 资产目录 Dao.
     */
    private final AssetFolderDao assetFolderDao;

    /**
     * 资产文件 Dao.
     */
    private final AssetFileDao assetFileDao;

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
     * 创建目录.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @param parentId       父级目录id
     * @param name           目录名称
     * @return 目录树响应参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetFolderTreeRespVO create(
            final BigInteger userId,
            final BigInteger conversationId,
            final BigInteger parentId,
            final String name
    ) {
        ConversationAssetAccess access = assetAccessGrpcClient.getAccess(conversationId, userId);
        AssetFolder parentFolder = Objects.isNull(parentId)
                ? assetFolderDao.getOrCreateMemberHome(conversationId, userId)
                : assetFolderDao.getFolderOrRoot(conversationId, parentId);
        AssertUtils.nonNull(parentFolder, BizErrorEnum.ASSET_FOLDER_NOT_FOUND);
        AssertUtils.isFalse(parentFolder.isMemberRoot(), BizErrorEnum.ASSET_TARGET_FOLDER_INVALID);
        AssertUtils.isTrue(parentFolder.canWriteContent(access), BizErrorEnum.ASSET_PERMISSION_DENIED);
        if (parentFolder.isRootFolder()) {
            boolean reservedName = AssetFolderConstant.MEMBER_ROOT_FOLDER_DISPLAY_NAME.equalsIgnoreCase(name)
                    || AssetFolderConstant.MEMBER_ROOT_FOLDER_NAME.equalsIgnoreCase(name);
            AssertUtils.isFalse(reservedName, BizErrorEnum.ASSET_TARGET_FOLDER_INVALID);
        }
        assetFolderDao.lockParentName(conversationId, parentFolder.getId());
        String availableName = AssetNameGenerator.getAvailableFolderName(
                name,
                assetFolderDao.getNormalNameSetByParentId(conversationId, parentFolder.getId())
        );
        AssetFolder folder = assetFolderDao.createFolder(conversationId, parentFolder, availableName, userId);
        return toTreeRespVO(folder, access, Map.of());
    }

    /**
     * 查询目录树.
     *
     * @param userId         用户id
     * @param conversationId 会话id
     * @return 目录树响应参数
     */
    @Override
    public AssetFolderTreeRespVO getTree(final BigInteger userId, final BigInteger conversationId) {
        ConversationAssetAccess access = assetAccessGrpcClient.getAccess(conversationId, userId);
        AssetFolder rootFolder = assetFolderDao.getRootFolder(conversationId);
        List<AssetFolder> folderList = assetFolderDao.getFolderListByConversationId(conversationId);
        List<BigInteger> ownerUserIdList = folderList.stream()
                .filter(AssetFolder::isMemberHome)
                .map(AssetFolder::getOwnerUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<BigInteger, String> usernameMap = assetAccessGrpcClient.getUsernameMap(ownerUserIdList);
        AssetFolderTreeRespVO rootRespVO = toTreeRespVO(rootFolder, access, usernameMap);
        Map<BigInteger, AssetFolderTreeRespVO> folderMap = folderList.stream()
                .map(folder -> toTreeRespVO(folder, access, usernameMap))
                .collect(Collectors.toMap(AssetFolderTreeRespVO::getId, Function.identity()));
        folderMap.put(rootRespVO.getId(), rootRespVO);
        folderList.forEach(folder -> fillChildFolder(folder, folderMap));
        return rootRespVO;
    }

    /**
     * 移动目录.
     *
     * @param userId         用户id
     * @param folderId       目录id
     * @param targetParentId 目标父级目录id
     * @return 目录树响应参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetFolderTreeRespVO move(
            final BigInteger userId,
            final BigInteger folderId,
            final BigInteger targetParentId
    ) {
        AssetFolder folder = assetFolderDao.getNormalFolderById(folderId);
        AssertUtils.nonNull(folder, BizErrorEnum.ASSET_FOLDER_NOT_FOUND);
        ConversationAssetAccess access = assetAccessGrpcClient.getAccess(folder.getConversationId(), userId);
        AssertUtils.isFalse(folder.isSystemFolder(), BizErrorEnum.ASSET_SYSTEM_FOLDER_IMMUTABLE);
        AssertUtils.isTrue(folder.canManage(access), BizErrorEnum.ASSET_PERMISSION_DENIED);
        AssetFolder targetParentFolder = assetFolderDao.getFolderOrRoot(folder.getConversationId(), targetParentId);
        AssertUtils.nonNull(targetParentFolder, BizErrorEnum.ASSET_FOLDER_NOT_FOUND);
        AssertUtils.isFalse(targetParentFolder.isMemberRoot(), BizErrorEnum.ASSET_TARGET_FOLDER_INVALID);
        AssertUtils.isTrue(targetParentFolder.canAcceptMove(access), BizErrorEnum.ASSET_PERMISSION_DENIED);
        AssertUtils.isFalse(folder.isSelfOrChild(targetParentFolder), BizErrorEnum.ASSET_FOLDER_MOVE_INVALID);
        String targetName = folder.getName();
        if (!Objects.equals(folder.getParentId(), targetParentFolder.getId())) {
            if (targetParentFolder.isRootFolder()) {
                boolean reservedName = AssetFolderConstant.MEMBER_ROOT_FOLDER_DISPLAY_NAME
                        .equalsIgnoreCase(targetName)
                        || AssetFolderConstant.MEMBER_ROOT_FOLDER_NAME.equalsIgnoreCase(targetName);
                AssertUtils.isFalse(reservedName, BizErrorEnum.ASSET_TARGET_FOLDER_INVALID);
            }
            assetFolderDao.lockParentName(folder.getConversationId(), targetParentFolder.getId());
            targetName = AssetNameGenerator.getAvailableFolderName(
                    folder.getName(),
                    assetFolderDao.getNormalNameSetByParentId(
                            folder.getConversationId(),
                            targetParentFolder.getId()
                    )
            );
        }
        assetFolderDao.moveFolderTree(folder, targetParentFolder, targetName, userId);
        return getTree(userId, folder.getConversationId());
    }

    /**
     * 重命名目录.
     *
     * @param userId   用户id
     * @param folderId 目录id
     * @param name     目录名称
     * @return 目录树响应参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetFolderTreeRespVO rename(final BigInteger userId, final BigInteger folderId, final String name) {
        AssetFolder folder = assetFolderDao.getNormalFolderById(folderId);
        AssertUtils.nonNull(folder, BizErrorEnum.ASSET_FOLDER_NOT_FOUND);
        ConversationAssetAccess access = assetAccessGrpcClient.getAccess(folder.getConversationId(), userId);
        AssertUtils.isFalse(folder.isSystemFolder(), BizErrorEnum.ASSET_SYSTEM_FOLDER_IMMUTABLE);
        AssertUtils.isTrue(folder.canManage(access), BizErrorEnum.ASSET_PERMISSION_DENIED);
        if (!Objects.equals(folder.getName(), name)) {
            if (AssetFolderConstant.ROOT_FOLDER_ID.equals(folder.getParentId())) {
                boolean reservedName = AssetFolderConstant.MEMBER_ROOT_FOLDER_DISPLAY_NAME.equalsIgnoreCase(name)
                        || AssetFolderConstant.MEMBER_ROOT_FOLDER_NAME.equalsIgnoreCase(name);
                AssertUtils.isFalse(reservedName, BizErrorEnum.ASSET_TARGET_FOLDER_INVALID);
            }
            assetFolderDao.lockParentName(folder.getConversationId(), folder.getParentId());
            AssertUtils.isFalse(
                    assetFolderDao.existsNormalName(folder.getConversationId(), folder.getParentId(), name),
                    BizErrorEnum.ASSET_NAME_DUPLICATE
            );
        }
        assetFolderDao.renameFolder(folder, name, userId);
        return getTree(userId, folder.getConversationId());
    }

    /**
     * 删除目录.
     *
     * @param userId   用户id
     * @param folderId 目录id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(final BigInteger userId, final BigInteger folderId) {
        AssetFolder folder = assetFolderDao.getNormalFolderById(folderId);
        AssertUtils.nonNull(folder, BizErrorEnum.ASSET_FOLDER_NOT_FOUND);
        ConversationAssetAccess access = assetAccessGrpcClient.getAccess(folder.getConversationId(), userId);
        AssertUtils.isFalse(folder.isSystemFolder(), BizErrorEnum.ASSET_SYSTEM_FOLDER_IMMUTABLE);
        AssertUtils.isTrue(folder.canManage(access), BizErrorEnum.ASSET_PERMISSION_DENIED);
        List<AssetFolder> folderList = assetFolderDao.getFolderTreeList(folder);
        List<BigInteger> folderIdList = folderList.stream().map(AssetFolder::getId).toList();
        List<AssetFile> assetFileList = assetFileDao.getFileListByFolderIdList(folderIdList);
        assetFolderDao.deleteFolderTree(folder, userId);
        assetFileDao.deleteFileList(assetFileList, userId);
        Date occurredAt = new Date();
        assetFileList.forEach(assetFile -> assetOutboxDao.createDeletedEvent(assetFile, occurredAt));
        Map<String, List<String>> storageKeyListMap = assetFileList.stream()
                .collect(Collectors.groupingBy(
                        AssetFile::getBucketName,
                        Collectors.mapping(AssetFile::getStorageKey, Collectors.toList())
                ));
        storageKeyListMap.forEach(objectStorageService::removeObjectList);
    }

    /**
     * 填充子目录.
     *
     * @param folder    当前目录
     * @param folderMap 目录映射
     */
    private void fillChildFolder(
            final AssetFolder folder,
            final Map<BigInteger, AssetFolderTreeRespVO> folderMap
    ) {
        AssetFolderTreeRespVO parentRespVO = folderMap.get(folder.getParentId());
        AssetFolderTreeRespVO currentRespVO = folderMap.get(folder.getId());
        if (Objects.nonNull(parentRespVO) && Objects.nonNull(currentRespVO)) {
            parentRespVO.getChildFolderList().add(currentRespVO);
        }
    }

    /**
     * 转换目录树响应参数.
     *
     * @param folder      目录
     * @param access      会话资产访问上下文
     * @param usernameMap 用户名映射
     * @return 目录树响应参数
     */
    private AssetFolderTreeRespVO toTreeRespVO(
            final AssetFolder folder,
            final ConversationAssetAccess access,
            final Map<BigInteger, String> usernameMap
    ) {
        String displayName;
        if (folder.isMemberRoot()) {
            displayName = AssetFolderConstant.MEMBER_ROOT_FOLDER_DISPLAY_NAME;
        } else if (folder.isMemberHome()) {
            displayName = usernameMap.getOrDefault(
                    folder.getOwnerUserId(),
                    folder.getOwnerUserId().toString()
            );
        } else {
            displayName = folder.getName();
        }
        return AssetFolderTreeRespVO.builder()
                .id(folder.getId())
                .conversationId(folder.getConversationId())
                .parentId(folder.getParentId())
                .name(displayName)
                .levelNo(folder.getLevelNo())
                .folderType(AssetFolderTypeEnum.valueOf(folder.getFolderType()))
                .ownerUserId(folder.getOwnerUserId())
                .permissions(AssetFolderPermissionRespVO.from(access, folder))
                .build();
    }
}
