package com.devin.uniontalk.file.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devin.uniontalk.file.domain.model.ConversationAssetAccess;
import com.devin.uniontalk.infrastructure.file.constant.AssetFolderConstant;
import com.devin.uniontalk.infrastructure.file.enums.AssetFolderStatusEnum;
import com.devin.uniontalk.infrastructure.file.enums.AssetFolderTypeEnum;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import lombok.Data;

/**
 * 2026/06/30 18:20.
 *
 * <p>
 * 资产目录(AssetFolder)Entity层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_asset_folder")
public class AssetFolder implements Serializable {

    /**
     * 序列化版本号.
     */
    @Serial
    private static final long serialVersionUID = -58956394062388220L;

    /**
     * 主键id.
     */
    @TableId
    private BigInteger id;

    /**
     * 会话id.
     */
    @TableField("conversation_id")
    private BigInteger conversationId;

    /**
     * 父级目录id.
     */
    @TableField("parent_id")
    private BigInteger parentId;

    /**
     * 目录名称.
     */
    @TableField("name")
    private String name;

    /**
     * 文件目录id路径.
     */
    @TableField("path_ids")
    private String pathIds;

    /**
     * 目录层级.
     */
    @TableField("level_no")
    private Integer levelNo;

    /**
     * 排序.
     */
    @TableField("sort_no")
    private Integer sortNo;

    /**
     * 目录类型.
     */
    @TableField("folder_type")
    private String folderType;

    /**
     * 成员空间所有者用户id.
     */
    @TableField("owner_user_id")
    private BigInteger ownerUserId;

    /**
     * 目录状态.
     */
    @TableField("status")
    private String status;

    /**
     * 删除时间.
     */
    @TableField("deleted_at")
    private Date deletedAt;

    /**
     * 创建人.
     */
    @TableField("created_by")
    private BigInteger createdBy;

    /**
     * 更新人.
     */
    @TableField("updated_by")
    private BigInteger updatedBy;

    /**
     * 创建时间.
     */
    @TableField("created_at")
    private Date createdAt;

    /**
     * 更新时间.
     */
    @TableField("updated_at")
    private Date updatedAt;

    /**
     * 初始化虚拟根目录.
     *
     * @param conversationId 会话id
     */
    public void initVirtualRoot(final BigInteger conversationId) {
        this.id = AssetFolderConstant.ROOT_FOLDER_ID;
        this.conversationId = conversationId;
        this.parentId = AssetFolderConstant.ROOT_PARENT_ID;
        this.name = AssetFolderConstant.ROOT_FOLDER_NAME;
        this.pathIds = AssetFolderConstant.PATH_SEPARATOR;
        this.levelNo = 0;
        this.sortNo = 0;
        this.folderType = AssetFolderTypeEnum.VIRTUAL_ROOT.name();
        this.ownerUserId = null;
        this.status = AssetFolderStatusEnum.NORMAL.name();
    }

    /**
     * 初始化子目录.
     *
     * @param id             目录id
     * @param conversationId 会话id
     * @param parentFolder   父级目录
     * @param name           目录名称
     * @param userId         操作用户id
     */
    public void initChild(
            final BigInteger id,
            final BigInteger conversationId,
            final AssetFolder parentFolder,
            final String name,
            final BigInteger userId
    ) {
        Date now = new Date();
        this.id = id;
        this.conversationId = conversationId;
        this.parentId = parentFolder.getId();
        this.name = name;
        this.pathIds = parentFolder.getPathIds() + id + AssetFolderConstant.PATH_SEPARATOR;
        this.levelNo = parentFolder.getLevelNo() + 1;
        this.sortNo = 0;
        this.folderType = AssetFolderTypeEnum.NORMAL.name();
        this.ownerUserId = parentFolder.getOwnerUserId();
        this.status = AssetFolderStatusEnum.NORMAL.name();
        this.createdBy = userId;
        this.updatedBy = userId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 初始化成员目录容器.
     *
     * @param id             目录id
     * @param conversationId 会话id
     * @param userId         操作用户id
     */
    public void initMemberRoot(
            final BigInteger id,
            final BigInteger conversationId,
            final BigInteger userId
    ) {
        Date now = new Date();
        this.id = id;
        this.conversationId = conversationId;
        this.parentId = AssetFolderConstant.ROOT_FOLDER_ID;
        this.name = AssetFolderConstant.MEMBER_ROOT_FOLDER_NAME;
        this.pathIds = AssetFolderConstant.PATH_SEPARATOR + id + AssetFolderConstant.PATH_SEPARATOR;
        this.levelNo = 1;
        this.sortNo = 0;
        this.folderType = AssetFolderTypeEnum.MEMBER_ROOT.name();
        this.ownerUserId = null;
        this.status = AssetFolderStatusEnum.NORMAL.name();
        this.createdBy = userId;
        this.updatedBy = userId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 初始化成员个人根目录.
     *
     * @param id          目录id
     * @param memberRoot 成员目录容器
     * @param ownerUserId 空间所有者用户id
     */
    public void initMemberHome(
            final BigInteger id,
            final AssetFolder memberRoot,
            final BigInteger ownerUserId
    ) {
        Date now = new Date();
        this.id = id;
        this.conversationId = memberRoot.getConversationId();
        this.parentId = memberRoot.getId();
        this.name = ownerUserId.toString();
        this.pathIds = memberRoot.getPathIds() + id + AssetFolderConstant.PATH_SEPARATOR;
        this.levelNo = memberRoot.getLevelNo() + 1;
        this.sortNo = 0;
        this.folderType = AssetFolderTypeEnum.MEMBER_HOME.name();
        this.ownerUserId = ownerUserId;
        this.status = AssetFolderStatusEnum.NORMAL.name();
        this.createdBy = ownerUserId;
        this.updatedBy = ownerUserId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 重命名目录.
     *
     * @param newName 新目录名称
     * @param userId  操作用户id
     */
    public void rename(final String newName, final BigInteger userId) {
        this.name = newName;
        this.updatedBy = userId;
        this.updatedAt = new Date();
    }

    /**
     * 移动目录到新父目录.
     *
     * @param targetParentFolder 目标父目录
     * @param targetName         目标目录名称
     * @param userId             操作用户id
     */
    public void moveTo(
            final AssetFolder targetParentFolder,
            final String targetName,
            final BigInteger userId
    ) {
        String oldPathIds = this.pathIds;
        String newPathIds = targetParentFolder.getPathIds() + this.id + AssetFolderConstant.PATH_SEPARATOR;
        int levelDelta = targetParentFolder.getLevelNo() + 1 - this.levelNo;
        changePathPrefix(
                oldPathIds,
                newPathIds,
                levelDelta,
                targetParentFolder.getOwnerUserId(),
                userId
        );
        this.parentId = targetParentFolder.getId();
        this.name = targetName;
    }

    /**
     * 替换目录路径前缀.
     *
     * @param oldPathPrefix 旧路径前缀
     * @param newPathPrefix 新路径前缀
     * @param levelDelta    层级差值
     * @param ownerUserId   目标空间所有者用户id
     * @param userId        操作用户id
     */
    public void changePathPrefix(
            final String oldPathPrefix,
            final String newPathPrefix,
            final int levelDelta,
            final BigInteger ownerUserId,
            final BigInteger userId
    ) {
        this.pathIds = newPathPrefix + this.pathIds.substring(oldPathPrefix.length());
        this.levelNo = this.levelNo + levelDelta;
        this.ownerUserId = ownerUserId;
        this.updatedBy = userId;
        this.updatedAt = new Date();
    }

    /**
     * 标记目录已删除.
     *
     * @param userId 操作用户id
     */
    public void markDeleted(final BigInteger userId) {
        Date now = new Date();
        this.status = AssetFolderStatusEnum.DELETED.name();
        this.deletedAt = now;
        this.updatedBy = userId;
        this.updatedAt = now;
    }

    /**
     * 判断是否根目录.
     *
     * @return true表示根目录
     */
    public boolean isRootFolder() {
        return AssetFolderConstant.ROOT_FOLDER_ID.equals(id);
    }

    /**
     * 判断是否成员目录容器.
     *
     * @return true表示成员目录容器
     */
    public boolean isMemberRoot() {
        return AssetFolderTypeEnum.MEMBER_ROOT.name().equals(folderType);
    }

    /**
     * 判断是否成员个人根目录.
     *
     * @return true表示成员个人根目录
     */
    public boolean isMemberHome() {
        return AssetFolderTypeEnum.MEMBER_HOME.name().equals(folderType);
    }

    /**
     * 判断是否系统目录.
     *
     * @return true表示系统目录
     */
    public boolean isSystemFolder() {
        return isRootFolder() || isMemberRoot() || isMemberHome();
    }

    /**
     * 判断是否可以写入当前目录内容.
     *
     * @param access 会话资产访问上下文
     * @return true表示可以写入
     */
    public boolean canWriteContent(final ConversationAssetAccess access) {
        return !isMemberRoot() && access.canWrite(ownerUserId);
    }

    /**
     * 判断是否可以管理当前目录.
     *
     * @param access 会话资产访问上下文
     * @return true表示可以管理
     */
    public boolean canManage(final ConversationAssetAccess access) {
        return AssetFolderTypeEnum.NORMAL.name().equals(folderType) && access.canWrite(ownerUserId);
    }

    /**
     * 判断是否可以接收移动内容.
     *
     * @param access 会话资产访问上下文
     * @return true表示可以接收
     */
    public boolean canAcceptMove(final ConversationAssetAccess access) {
        return !isMemberRoot() && access.canWrite(ownerUserId);
    }

    /**
     * 判断目标目录是否当前目录或子孙目录.
     *
     * @param targetFolder 目标目录
     * @return true表示目标非法
     */
    public boolean isSelfOrChild(final AssetFolder targetFolder) {
        return targetFolder.getPathIds().startsWith(this.pathIds);
    }
}
