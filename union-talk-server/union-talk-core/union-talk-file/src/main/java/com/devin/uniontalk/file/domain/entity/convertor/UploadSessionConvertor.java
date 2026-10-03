package com.devin.uniontalk.file.domain.entity.convertor;

import com.devin.uniontalk.file.domain.entity.UploadSession;
import com.devin.uniontalk.file.domain.vo.resp.UploadSessionRespVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 2026/06/30 18:45.
 *
 * <p>
 * 上传任务实体转换器
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
public interface UploadSessionConvertor {

    /**
     * 上传任务转换器实例.
     */
    UploadSessionConvertor INSTANCE = Mappers.getMapper(UploadSessionConvertor.class);

    /**
     * 将上传任务实体转换为响应VO.
     *
     * @param session        上传任务实体
     * @param chunkIndexList 已上传分片序号列表
     * @return 上传任务响应VO
     */
    @Mappings({
        @Mapping(source = "session.id", target = "sessionId"),
        @Mapping(source = "chunkIndexList", target = "chunkIndexList")
    })
    UploadSessionRespVO toRespVO(UploadSession session, List<Integer> chunkIndexList);
}
