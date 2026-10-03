package com.devin.uniontalk.file.domain.vo.resp;

import com.devin.uniontalk.file.domain.entity.AssetFolder;
import com.devin.uniontalk.file.domain.model.ConversationAssetAccess;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/07/22 11:30.
 *
 * <p>
 * 资产目录权限响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "资产目录权限响应参数")
public class AssetFolderPermissionRespVO {

    /**
     * 是否可以查看.
     */
    @Schema(description = "是否可以查看")
    private Boolean canView;

    /**
     * 是否可以上传文件.
     */
    @Schema(description = "是否可以上传文件")
    private Boolean canUpload;

    /**
     * 是否可以创建子目录.
     */
    @Schema(description = "是否可以创建子目录")
    private Boolean canCreateFolder;

    /**
     * 是否可以重命名.
     */
    @Schema(description = "是否可以重命名")
    private Boolean canRename;

    /**
     * 是否可以移动.
     */
    @Schema(description = "是否可以移动")
    private Boolean canMove;

    /**
     * 是否可以删除.
     */
    @Schema(description = "是否可以删除")
    private Boolean canDelete;

    /**
     * 是否可以接收移动内容.
     */
    @Schema(description = "是否可以接收移动内容")
    private Boolean canAcceptMove;

    /**
     * 根据当前访问上下文构建目录权限.
     *
     * @param access 会话资产访问上下文
     * @param folder 资产目录
     * @return 目录权限响应参数
     */
    public static AssetFolderPermissionRespVO from(
            final ConversationAssetAccess access,
            final AssetFolder folder
    ) {
        boolean canWriteContent = folder.canWriteContent(access);
        boolean canManage = folder.canManage(access);
        return AssetFolderPermissionRespVO.builder()
                .canView(Boolean.TRUE)
                .canUpload(canWriteContent)
                .canCreateFolder(canWriteContent)
                .canRename(canManage)
                .canMove(canManage)
                .canDelete(canManage)
                .canAcceptMove(folder.canAcceptMove(access))
                .build();
    }
}
