package com.devin.uniontalk.user.domain.entity.convertor;

import com.devin.uniontalk.user.domain.entity.Group;
import com.devin.uniontalk.user.domain.vo.resp.GroupRespVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 2026/05/17 16:00.
 *
 * <p>
 * 群聊实体转换器
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
public interface GroupConvertor {

    GroupConvertor INSTANCE = Mappers.getMapper(GroupConvertor.class);

    /**
     * 将群聊实体转换为响应VO（不含成员数量）.
     *
     * @param group 群聊实体
     * @return 群聊响应VO
     */
    @Mapping(target = "memberCount", ignore = true)
    GroupRespVO toRespVO(Group group);
}
