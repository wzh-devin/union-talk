package com.devin.uniontalk.user.domain.entity.convertor;

import com.devin.uniontalk.user.domain.entity.FriendGroup;
import com.devin.uniontalk.user.domain.vo.resp.FriendGroupRespVO;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import java.util.List;

/**
 * 2026/05/17 16:00.
 *
 * <p>
 * 好友分组实体转换器
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
public interface FriendGroupConvertor {

    FriendGroupConvertor INSTANCE = Mappers.getMapper(FriendGroupConvertor.class);

    /**
     * 将好友分组实体转换为响应VO.
     *
     * @param friendGroup 好友分组实体
     * @return 好友分组响应VO
     */
    FriendGroupRespVO toRespVO(FriendGroup friendGroup);

    /**
     * 批量将好友分组实体转换为响应VO.
     *
     * @param friendGroups 好友分组实体列表
     * @return 好友分组响应VO列表
     */
    List<FriendGroupRespVO> toRespVOList(List<FriendGroup> friendGroups);
}
