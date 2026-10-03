package com.devin.uniontalk.file.mapper;

import com.devin.uniontalk.file.domain.entity.UploadChunk;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 2026/06/30 15:21:00.
 *
 * <p>
 *  上传分片表(UploadChunk)Mapper层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper
public interface UploadChunkMapper extends BaseMapper<UploadChunk> {

}
