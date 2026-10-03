package com.devin.uniontalk.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devin.uniontalk.file.domain.entity.AssetFolder;
import java.math.BigInteger;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 2026/06/30 15:20:59.
 *
 * <p>
 *  资产目录(AssetFolder)Mapper层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper
public interface AssetFolderMapper extends BaseMapper<AssetFolder> {

    /**
     * 系统目录不存在时插入.
     *
     * @param folder 系统目录
     * @return 受影响行数
     */
    @Insert("""
            INSERT INTO ut_asset_folder (
                id, conversation_id, parent_id, name, path_ids, level_no, sort_no,
                folder_type, owner_user_id, status, created_by, updated_by, created_at, updated_at
            ) VALUES (
                #{folder.id}, #{folder.conversationId}, #{folder.parentId}, #{folder.name},
                #{folder.pathIds}, #{folder.levelNo}, #{folder.sortNo}, #{folder.folderType},
                #{folder.ownerUserId}, #{folder.status}, #{folder.createdBy}, #{folder.updatedBy},
                #{folder.createdAt}, #{folder.updatedAt}
            )
            ON CONFLICT DO NOTHING
            """)
    int insertSystemFolderIfAbsent(@Param("folder") AssetFolder folder);

    /**
     * 获取父目录名称事务锁.
     *
     * @param conversationId 会话id
     * @param parentId       父目录id
     * @return 锁定结果
     */
    @Select("""
            SELECT 1
            FROM pg_advisory_xact_lock(
                hashtextextended(CONCAT('asset-folder-name:', #{conversationId}, ':', #{parentId}), 0)
            )
            """)
    Integer lockParentName(
            @Param("conversationId") BigInteger conversationId,
            @Param("parentId") BigInteger parentId
    );
}
