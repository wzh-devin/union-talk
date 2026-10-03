package com.devin.uniontalk.file.domain.entity.convertor;

import com.devin.uniontalk.file.domain.entity.AssetFile;
import com.devin.uniontalk.file.domain.entity.AssetFolder;
import com.devin.uniontalk.file.domain.model.ConversationAssetAccess;
import com.devin.uniontalk.file.domain.vo.resp.AssetFilePermissionRespVO;
import com.devin.uniontalk.file.domain.vo.resp.AssetFileRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 2026/07/23 11:09.
 *
 * <p>
 * 资产文件实体转换器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper(
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AssetFileConvertor {

    /**
     * 资产文件转换器实例.
     */
    AssetFileConvertor INSTANCE = Mappers.getMapper(AssetFileConvertor.class);

    /**
     * 将资产文件实体转换为响应参数.
     *
     * @param assetFile 资产文件实体
     * @return 资产文件响应参数
     */
    @Mapping(target = "permissions", ignore = true)
    AssetFileRespVO toRespVO(AssetFile assetFile);

    /**
     * 将资产文件实体转换为包含当前权限的响应参数.
     *
     * @param assetFile 资产文件实体
     * @param access    会话资产访问上下文
     * @param folder    文件所在目录
     * @return 资产文件响应参数
     */
    default AssetFileRespVO toRespVO(
            final AssetFile assetFile,
            final ConversationAssetAccess access,
            final AssetFolder folder
    ) {
        AssetFileRespVO respVO = toRespVO(assetFile);
        respVO.setPermissions(AssetFilePermissionRespVO.from(access, folder));
        return respVO;
    }

    /**
     * 批量将资产文件实体转换为响应参数.
     *
     * @param assetFileList 资产文件实体列表
     * @return 资产文件响应参数列表
     */
    List<AssetFileRespVO> toRespVOList(List<AssetFile> assetFileList);

    /**
     * 批量将资产文件实体转换为包含当前权限的响应参数.
     *
     * @param assetFileList 资产文件实体列表
     * @param access        会话资产访问上下文
     * @param folder        文件所在目录
     * @return 资产文件响应参数列表
     */
    default List<AssetFileRespVO> toRespVOList(
            final List<AssetFile> assetFileList,
            final ConversationAssetAccess access,
            final AssetFolder folder
    ) {
        return assetFileList.stream()
                .map(assetFile -> toRespVO(assetFile, access, folder))
                .toList();
    }
}
