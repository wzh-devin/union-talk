package com.devin.uniontalk.file.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.utils.AssertUtils;
import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.file.domain.entity.AssetFolder;
import com.devin.uniontalk.file.mapper.AssetFolderMapper;
import com.devin.uniontalk.infrastructure.file.constant.AssetFolderConstant;
import com.devin.uniontalk.infrastructure.file.enums.AssetFolderStatusEnum;
import com.devin.uniontalk.infrastructure.file.enums.AssetFolderTypeEnum;
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
 * 资产目录(AssetFolder)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetFolderDao extends ServiceImpl<AssetFolderMapper, AssetFolder> {

    /**
     * 查询会话虚拟根目录.
     *
     * @param conversationId 会话id
     * @return 根目录
     */
    public AssetFolder getRootFolder(final BigInteger conversationId) {
        AssetFolder folder = new AssetFolder();
        folder.initVirtualRoot(conversationId);
        return folder;
    }

    /**
     * 查询正常目录.
     *
     * @param folderId 目录id
     * @return 目录
     */
    public AssetFolder getNormalFolderById(final BigInteger folderId) {
        if (Objects.isNull(folderId) || AssetFolderConstant.ROOT_FOLDER_ID.equals(folderId)) {
            return null;
        }
        return lambdaQuery()
                .eq(AssetFolder::getId, folderId)
                .and(folderQuery -> folderQuery
                        .ne(AssetFolder::getParentId, AssetFolderConstant.ROOT_PARENT_ID)
                        .or()
                        .ne(AssetFolder::getName, AssetFolderConstant.ROOT_FOLDER_NAME))
                .eq(AssetFolder::getStatus, AssetFolderStatusEnum.NORMAL.name())
                .one();
    }

    /**
     * 查询指定目录或会话根目录.
     *
     * @param conversationId 会话id
     * @param folderId       目录id
     * @return 目录
     */
    public AssetFolder getFolderOrRoot(
            final BigInteger conversationId,
            final BigInteger folderId
    ) {
        if (Objects.isNull(folderId) || AssetFolderConstant.ROOT_FOLDER_ID.equals(folderId)) {
            return getRootFolder(conversationId);
        }
        AssetFolder folder = getNormalFolderById(folderId);
        return Objects.nonNull(folder) && Objects.equals(conversationId, folder.getConversationId())
                ? folder
                : null;
    }

    /**
     * 查询成员目录容器.
     *
     * @param conversationId 会话id
     * @return 成员目录容器
     */
    public AssetFolder getMemberRoot(final BigInteger conversationId) {
        return lambdaQuery()
                .eq(AssetFolder::getConversationId, conversationId)
                .eq(AssetFolder::getFolderType, AssetFolderTypeEnum.MEMBER_ROOT.name())
                .eq(AssetFolder::getStatus, AssetFolderStatusEnum.NORMAL.name())
                .one();
    }

    /**
     * 查询成员个人根目录.
     *
     * @param conversationId 会话id
     * @param ownerUserId    空间所有者用户id
     * @return 成员个人根目录
     */
    public AssetFolder getMemberHome(
            final BigInteger conversationId,
            final BigInteger ownerUserId
    ) {
        return lambdaQuery()
                .eq(AssetFolder::getConversationId, conversationId)
                .eq(AssetFolder::getFolderType, AssetFolderTypeEnum.MEMBER_HOME.name())
                .eq(AssetFolder::getOwnerUserId, ownerUserId)
                .eq(AssetFolder::getStatus, AssetFolderStatusEnum.NORMAL.name())
                .one();
    }

    /**
     * 查询或创建成员个人根目录.
     *
     * @param conversationId 会话id
     * @param userId         用户id
     * @return 成员个人根目录
     */
    public AssetFolder getOrCreateMemberHome(
            final BigInteger conversationId,
            final BigInteger userId
    ) {
        AssetFolder memberRoot = getMemberRoot(conversationId);
        if (Objects.isNull(memberRoot)) {
            AssetFolder newMemberRoot = new AssetFolder();
            newMemberRoot.initMemberRoot(IdGenerator.nextIdBigInteger(), conversationId, userId);
            getBaseMapper().insertSystemFolderIfAbsent(newMemberRoot);
            memberRoot = getMemberRoot(conversationId);
        }
        AssertUtils.nonNull(memberRoot, BizErrorEnum.ASSET_FOLDER_NOT_FOUND);

        AssetFolder memberHome = getMemberHome(conversationId, userId);
        if (Objects.isNull(memberHome)) {
            AssetFolder newMemberHome = new AssetFolder();
            newMemberHome.initMemberHome(IdGenerator.nextIdBigInteger(), memberRoot, userId);
            getBaseMapper().insertSystemFolderIfAbsent(newMemberHome);
            memberHome = getMemberHome(conversationId, userId);
        }
        AssertUtils.nonNull(memberHome, BizErrorEnum.ASSET_FOLDER_NOT_FOUND);
        return memberHome;
    }

    /**
     * 创建目录.
     *
     * @param conversationId 会话id
     * @param parentFolder   父级目录
     * @param name           目录名称
     * @param userId         操作用户id
     * @return 目录
     */
    public AssetFolder createFolder(
            final BigInteger conversationId,
            final AssetFolder parentFolder,
            final String name,
            final BigInteger userId
    ) {
        AssetFolder folder = new AssetFolder();
        folder.initChild(IdGenerator.nextIdBigInteger(), conversationId, parentFolder, name, userId);
        save(folder);
        return folder;
    }

    /**
     * 判断同级目录名称是否存在.
     *
     * @param conversationId 会话id
     * @param parentId       父级目录id
     * @param name           目录名称
     * @return true表示存在
     */
    public boolean existsNormalName(
            final BigInteger conversationId,
            final BigInteger parentId,
            final String name
    ) {
        return lambdaQuery()
                .eq(AssetFolder::getConversationId, conversationId)
                .eq(AssetFolder::getParentId, parentId)
                .eq(AssetFolder::getName, name)
                .eq(AssetFolder::getStatus, AssetFolderStatusEnum.NORMAL.name())
                .exists();
    }

    /**
     * 获取父目录名称事务锁.
     *
     * @param conversationId 会话id
     * @param parentId       父目录id
     */
    public void lockParentName(final BigInteger conversationId, final BigInteger parentId) {
        getBaseMapper().lockParentName(conversationId, parentId);
    }

    /**
     * 查询父目录下正常目录名称集合.
     *
     * @param conversationId 会话id
     * @param parentId       父目录id
     * @return 目录名称集合
     */
    public Set<String> getNormalNameSetByParentId(
            final BigInteger conversationId,
            final BigInteger parentId
    ) {
        return lambdaQuery()
                .select(AssetFolder::getName)
                .eq(AssetFolder::getConversationId, conversationId)
                .eq(AssetFolder::getParentId, parentId)
                .eq(AssetFolder::getStatus, AssetFolderStatusEnum.NORMAL.name())
                .list()
                .stream()
                .map(AssetFolder::getName)
                .collect(Collectors.toSet());
    }

    /**
     * 查询会话目录列表.
     *
     * @param conversationId 会话id
     * @return 目录列表
     */
    public List<AssetFolder> getFolderListByConversationId(final BigInteger conversationId) {
        return lambdaQuery()
                .eq(AssetFolder::getConversationId, conversationId)
                .and(folderQuery -> folderQuery
                        .ne(AssetFolder::getParentId, AssetFolderConstant.ROOT_PARENT_ID)
                        .or()
                        .ne(AssetFolder::getName, AssetFolderConstant.ROOT_FOLDER_NAME))
                .eq(AssetFolder::getStatus, AssetFolderStatusEnum.NORMAL.name())
                .orderByAsc(AssetFolder::getLevelNo)
                .orderByAsc(AssetFolder::getSortNo)
                .orderByAsc(AssetFolder::getCreatedAt)
                .list();
    }

    /**
     * 查询目录子树列表.
     *
     * @param folder 目录
     * @return 目录子树列表
     */
    public List<AssetFolder> getFolderTreeList(final AssetFolder folder) {
        return lambdaQuery()
                .eq(AssetFolder::getConversationId, folder.getConversationId())
                .likeRight(AssetFolder::getPathIds, folder.getPathIds())
                .and(folderQuery -> folderQuery
                        .ne(AssetFolder::getParentId, AssetFolderConstant.ROOT_PARENT_ID)
                        .or()
                        .ne(AssetFolder::getName, AssetFolderConstant.ROOT_FOLDER_NAME))
                .eq(AssetFolder::getStatus, AssetFolderStatusEnum.NORMAL.name())
                .orderByAsc(AssetFolder::getLevelNo)
                .orderByAsc(AssetFolder::getSortNo)
                .orderByAsc(AssetFolder::getCreatedAt)
                .list();
    }

    /**
     * 移动目录及子目录.
     *
     * @param folder             当前目录
     * @param targetParentFolder 目标父目录
     * @param targetName         目标目录名称
     * @param userId             操作用户id
     */
    public void moveFolderTree(
            final AssetFolder folder,
            final AssetFolder targetParentFolder,
            final String targetName,
            final BigInteger userId
    ) {
        String oldPathIds = folder.getPathIds();
        String newPathIds = targetParentFolder.getPathIds()
                + folder.getId()
                + AssetFolderConstant.PATH_SEPARATOR;
        int levelDelta = targetParentFolder.getLevelNo() + 1 - folder.getLevelNo();
        List<AssetFolder> folderList = getFolderTreeList(folder);
        folderList.forEach(currentFolder -> {
            if (Objects.equals(currentFolder.getId(), folder.getId())) {
                currentFolder.moveTo(targetParentFolder, targetName, userId);
                return;
            }
            currentFolder.changePathPrefix(
                    oldPathIds,
                    newPathIds,
                    levelDelta,
                    targetParentFolder.getOwnerUserId(),
                    userId
            );
        });
        updateBatchById(folderList);
    }

    /**
     * 重命名目录.
     *
     * @param folder 目录
     * @param name   新目录名称
     * @param userId 操作用户id
     */
    public void renameFolder(final AssetFolder folder, final String name, final BigInteger userId) {
        folder.rename(name, userId);
        updateById(folder);
    }

    /**
     * 标记目录子树已删除.
     *
     * @param folder 目录
     * @param userId 操作用户id
     */
    public void deleteFolderTree(final AssetFolder folder, final BigInteger userId) {
        List<AssetFolder> folderList = getFolderTreeList(folder);
        folderList.forEach(currentFolder -> currentFolder.markDeleted(userId));
        updateBatchById(folderList);
    }
}
