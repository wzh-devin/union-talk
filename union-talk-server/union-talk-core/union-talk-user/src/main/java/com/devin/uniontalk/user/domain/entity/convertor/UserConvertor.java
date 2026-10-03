package com.devin.uniontalk.user.domain.entity.convertor;

import com.devin.uniontalk.grpc.user.domain.model.UserInfoProto;
import com.devin.uniontalk.user.domain.entity.User;
import com.devin.uniontalk.user.domain.vo.resp.UserInfoRespVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 2026/5/14 22:48.
 *
 * <p>
 * UserConvertor
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
public interface UserConvertor {

    UserConvertor INSTANCE = Mappers.getMapper(UserConvertor.class);

    /**
     * 将用户实体转换为Grpc用户信息.
     *
     * @param user 用户实体
     * @return Grpc用户信息
     */
    @BeanMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    UserInfoProto toUserInfoProto(User user);

    /**
     * 将用户实体转换为用户信息响应VO.
     *
     * @param user 用户实体
     * @return 用户信息响应VO
     */
    @Mapping(source = "id", target = "uid")
    UserInfoRespVO toUserInfoRespVO(User user);

}
