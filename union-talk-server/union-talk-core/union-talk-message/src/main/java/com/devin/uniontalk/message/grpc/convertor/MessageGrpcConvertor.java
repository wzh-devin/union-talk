package com.devin.uniontalk.message.grpc.convertor;

import com.devin.uniontalk.grpc.file.domain.model.AssetFileInfo;
import com.devin.uniontalk.grpc.user.domain.model.ConversationGroupInfo;
import com.devin.uniontalk.grpc.user.domain.model.ConversationUserInfo;
import com.devin.uniontalk.message.domain.vo.resp.ConversationGroupInfoRespVO;
import com.devin.uniontalk.message.domain.vo.resp.ConversationUserInfoRespVO;
import com.devin.uniontalk.message.domain.vo.resp.MessageAssetInfoRespVO;
import java.math.BigInteger;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 2026/08/07.
 *
 * <p>
 * Message模块Grpc数据转换器
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper(
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MessageGrpcConvertor {

    /**
     * Message模块Grpc数据转换器实例.
     */
    MessageGrpcConvertor INSTANCE = Mappers.getMapper(MessageGrpcConvertor.class);

    /**
     * 将用户Grpc信息转换为会话用户响应参数.
     *
     * @param userInfo 用户Grpc信息
     * @return 会话用户响应参数
     */
    ConversationUserInfoRespVO toUserInfoRespVO(ConversationUserInfo userInfo);

    /**
     * 将群聊Grpc信息转换为会话群聊响应参数.
     *
     * @param groupInfo 群聊Grpc信息
     * @return 会话群聊响应参数
     */
    ConversationGroupInfoRespVO toGroupInfoRespVO(ConversationGroupInfo groupInfo);

    /**
     * 将资产文件Grpc信息转换为消息资产响应参数.
     *
     * @param assetFileInfo 资产文件Grpc信息
     * @return 消息资产响应参数
     */
    @Mapping(target = "id", source = "assetId")
    MessageAssetInfoRespVO toAssetInfoRespVO(AssetFileInfo assetFileInfo);

    /**
     * 将字符串数值转换为大整数.
     *
     * @param value 字符串数值
     * @return 大整数
     */
    default BigInteger toBigInteger(final String value) {
        return new BigInteger(value);
    }
}
