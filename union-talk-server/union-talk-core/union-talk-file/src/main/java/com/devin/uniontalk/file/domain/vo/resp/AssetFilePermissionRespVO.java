package com.devin.uniontalk.file.domain.vo.resp;

import com.devin.uniontalk.file.domain.entity.AssetFolder;
import com.devin.uniontalk.file.domain.model.ConversationAssetAccess;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/07/22 11:35.
 *
 * <p>
 * 资产文件权限响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "资产文件权限响应参数")
public class AssetFilePermissionRespVO {

    /**
     * 是否可以查看.
     */
    @Schema(description = "是否可以查看")
    private Boolean canView;

    /**
     * 是否可以预览.
     */
    @Schema(description = "是否可以预览")
    private Boolean canPreview;

    /**
     * 是否可以下载.
     */
    @Schema(description = "是否可以下载")
    private Boolean canDownload;

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
     * 根据当前访问上下文构建文件权限.
     *
     * @param access 会话资产访问上下文
     * @param folder 文件所在目录
     * @return 文件权限响应参数
     */
    public static AssetFilePermissionRespVO from(
            final ConversationAssetAccess access,
            final AssetFolder folder
    ) {
        boolean canManage = folder.canWriteContent(access);
        return AssetFilePermissionRespVO.builder()
                .canView(Boolean.TRUE)
                .canPreview(Boolean.TRUE)
                .canDownload(Boolean.TRUE)
                .canRename(canManage)
                .canMove(canManage)
                .canDelete(canManage)
                .build();
    }
}
