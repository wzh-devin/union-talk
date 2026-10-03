package com.devin.uniontalk.user.domain.entity.convertor;

import com.devin.uniontalk.user.domain.entity.FriendRequest;
import com.devin.uniontalk.user.domain.vo.resp.FriendRequestRespVO;
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
 * 好友申请实体转换器
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
public interface FriendRequestConvertor {

    FriendRequestConvertor INSTANCE = Mappers.getMapper(FriendRequestConvertor.class);

    /**
     * 将好友申请实体转换为响应VO（不含申请人详细信息）.
     *
     * @param friendRequest 好友申请实体
     * @return 好友申请响应VO
     */
    @Mapping(target = "fromUsername", ignore = true)
    @Mapping(target = "fromAvatarUrl", ignore = true)
    FriendRequestRespVO toRespVO(FriendRequest friendRequest);

    /**
     * 批量将好友申请实体转换为响应VO.
     *
     * @param friendRequests 好友申请实体列表
     * @return 好友申请响应VO列表
     */
    List<FriendRequestRespVO> toRespVOList(List<FriendRequest> friendRequests);
}
