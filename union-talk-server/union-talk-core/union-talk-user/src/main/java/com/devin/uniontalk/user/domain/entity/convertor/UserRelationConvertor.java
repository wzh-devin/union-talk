package com.devin.uniontalk.user.domain.entity.convertor;

import com.devin.uniontalk.user.domain.entity.UserRelation;
import com.devin.uniontalk.user.domain.vo.resp.FriendRespVO;
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
 * 用户关系实体转换器
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
public interface UserRelationConvertor {

    UserRelationConvertor INSTANCE = Mappers.getMapper(UserRelationConvertor.class);

    /**
     * 将用户关系实体转换为好友响应VO（不含对方用户详细信息）.
     *
     * @param userRelation 用户关系实体
     * @return 好友响应VO
     */
    @Mapping(source = "targetId", target = "userId")
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    FriendRespVO toRespVO(UserRelation userRelation);

    /**
     * 批量将用户关系实体转换为好友响应VO.
     *
     * @param userRelations 用户关系实体列表
     * @return 好友响应VO列表
     */
    List<FriendRespVO> toRespVOList(List<UserRelation> userRelations);
}
