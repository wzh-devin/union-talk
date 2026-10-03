package com.devin.uniontalk.file.domain.model;

import com.devin.uniontalk.file.domain.entity.AssetFile;
import com.devin.uniontalk.file.domain.entity.AssetFolder;

/**
 * 2026/07/23 11:00.
 *
 * <p>
 * 资产文件访问上下文
 * </p>
 *
 * @param assetFile    资产文件.
 * @param sourceFolder 文件所在源目录.
 * @param access       会话资产访问上下文.
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public record AssetFileAccessContext(AssetFile assetFile, AssetFolder sourceFolder, ConversationAssetAccess access) {

    /**
     * 判断是否可以写入源目录内容.
     *
     * @return true表示可以写入
     */
    public boolean canWriteContent() {
        return sourceFolder.canWriteContent(access);
    }
}
