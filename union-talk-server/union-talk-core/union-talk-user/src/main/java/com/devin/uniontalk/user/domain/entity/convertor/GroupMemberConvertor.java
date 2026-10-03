package com.devin.uniontalk.user.domain.entity.convertor;

import com.devin.uniontalk.user.domain.entity.GroupMember;
import com.devin.uniontalk.user.domain.vo.resp.GroupMemberRespVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import java.util.List;

/**
 * 2026/05/17 16:00.
 *
 * <p>
 * 群成员实体转换器
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
public interface GroupMemberConvertor {

    GroupMemberConvertor INSTANCE = Mappers.getMapper(GroupMemberConvertor.class);

    /**
     * 将群成员实体转换为响应VO（不含用户详细信息）.
     *
     * @param groupMember 群成员实体
     * @return 群成员响应VO
     */
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    GroupMemberRespVO toRespVO(GroupMember groupMember);

    /**
     * 批量将群成员实体转换为响应VO.
     *
     * @param groupMembers 群成员实体列表
     * @return 群成员响应VO列表
     */
    List<GroupMemberRespVO> toRespVOList(List<GroupMember> groupMembers);
}
