package com.devin.uniontalk.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devin.uniontalk.file.domain.entity.AssetFile;
import java.math.BigInteger;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 2026/06/30 15:20:56.
 *
 * <p>
 *  资产文件(AssetFile)Mapper层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper
public interface AssetFileMapper extends BaseMapper<AssetFile> {

    /**
     * 获取目录文件名称事务锁.
     *
     * @param conversationId 会话id
     * @param folderId       目录id
     * @return 锁定结果
     */
    @Select("""
            SELECT 1
            FROM pg_advisory_xact_lock(
                hashtextextended(CONCAT('asset-file-name:', #{conversationId}, ':', #{folderId}), 0)
            )
            """)
    Integer lockFolderName(
            @Param("conversationId") BigInteger conversationId,
            @Param("folderId") BigInteger folderId
    );

    /**
     * 查询缺少删除发件箱事件的已删除文件.
     *
     * @param deletedStatus 文件删除状态
     * @param deletedEventType 资产删除事件类型
     * @param batchSize 查询数量
     * @return 待补偿文件列表
     */
    List<AssetFile> selectDeletedWithoutOutboxList(
            @Param("deletedStatus") String deletedStatus,
            @Param("deletedEventType") String deletedEventType,
            @Param("batchSize") Integer batchSize
    );
}
