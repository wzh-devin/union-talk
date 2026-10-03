package com.devin.uniontalk.file.domain.vo.resp;

import com.devin.uniontalk.infrastructure.file.enums.AssetFolderTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/06/30 18:40.
 *
 * <p>
 * 资产目录树响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "资产目录树响应参数")
public class AssetFolderTreeRespVO {

    /**
     * 目录id.
     */
    @Schema(description = "目录id")
    private BigInteger id;

    /**
     * 会话id.
     */
    @Schema(description = "会话id")
    private BigInteger conversationId;

    /**
     * 父级目录id.
     */
    @Schema(description = "父级目录id")
    private BigInteger parentId;

    /**
     * 目录名称.
     */
    @Schema(description = "目录名称")
    private String name;

    /**
     * 目录层级.
     */
    @Schema(description = "目录层级")
    private Integer levelNo;

    /**
     * 目录类型.
     */
    @Schema(description = "目录类型")
    private AssetFolderTypeEnum folderType;

    /**
     * 成员空间所有者用户id.
     */
    @Schema(description = "成员空间所有者用户id")
    private BigInteger ownerUserId;

    /**
     * 当前用户目录权限.
     */
    @Schema(description = "当前用户目录权限")
    private AssetFolderPermissionRespVO permissions;

    /**
     * 子目录列表.
     */
    @Builder.Default
    @Schema(description = "子目录列表")
    private List<AssetFolderTreeRespVO> childFolderList = new ArrayList<>();
}
