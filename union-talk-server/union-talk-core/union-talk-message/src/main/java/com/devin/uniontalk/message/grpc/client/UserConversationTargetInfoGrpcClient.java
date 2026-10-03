package com.devin.uniontalk.message.grpc.client;

import com.devin.uniontalk.base.exception.BizErrorEnum;
import com.devin.uniontalk.base.exception.BizException;
import com.devin.uniontalk.grpc.user.domain.model.ConversationGroupInfo;
import com.devin.uniontalk.grpc.user.domain.model.ConversationUserInfo;
import com.devin.uniontalk.grpc.user.domain.request.ConversationTargetInfoRequest;
import com.devin.uniontalk.grpc.user.domain.response.ConversationTargetInfoResponse;
import com.devin.uniontalk.grpc.user.service.UserGrpcServiceGrpc;
import com.devin.uniontalk.message.domain.vo.resp.ConversationGroupInfoRespVO;
import com.devin.uniontalk.message.domain.vo.resp.ConversationUserInfoRespVO;
import com.devin.uniontalk.message.grpc.convertor.MessageGrpcConvertor;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * 2026/05/31 21:40.
 *
 * <p>
 * User会话目标信息Grpc客户端
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
public class UserConversationTargetInfoGrpcClient {

    /**
     * 用户Grpc阻塞调用客户端.
     */
    @GrpcClient("union-talk-user")
    private UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userGrpcServiceBlockingStub;

    /**
     * 查询会话目标信息.
     *
     * @param userIdList  用户id列表
     * @param groupIdList 群聊id列表
     * @return 会话目标信息
     */
    public TargetInfo getConversationTargetInfo(
            final List<BigInteger> userIdList,
            final List<BigInteger> groupIdList
    ) {
        if (isEmpty(userIdList) && isEmpty(groupIdList)) {
            return TargetInfo.empty();
        }
        try {
            ConversationTargetInfoResponse response = userGrpcServiceBlockingStub.getConversationTargetInfo(
                    ConversationTargetInfoRequest.newBuilder()
                            .addAllUserId(getStringList(userIdList))
                            .addAllGroupId(getStringList(groupIdList))
                            .build()
            );
            validateResponse(response, userIdList, groupIdList);
            return TargetInfo.builder()
                    .userInfoMap(toUserInfoMap(response.getUserInfoList()))
                    .groupInfoMap(toGroupInfoMap(response.getGroupInfoList()))
                    .build();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用User查询会话目标信息Grpc失败, userIdList={}, groupIdList={}", userIdList, groupIdList, e);
            throw new BizException(BizErrorEnum.CONVERSATION_TARGET_QUERY_FAILED);
        }
    }

    /**
     * 校验响应结果.
     *
     * @param response    会话目标信息响应
     * @param userIdList  用户id列表
     * @param groupIdList 群聊id列表
     */
    private void validateResponse(
            final ConversationTargetInfoResponse response,
            final List<BigInteger> userIdList,
            final List<BigInteger> groupIdList
    ) {
        if (Objects.isNull(response) || !response.getBr().getSuccess()) {
            log.warn(
                    "User查询会话目标信息失败, userIdList={}, groupIdList={}, code={}, message={}",
                    userIdList,
                    groupIdList,
                    Objects.nonNull(response) ? response.getBr().getCode() : null,
                    Objects.nonNull(response) ? response.getBr().getMessage() : null
            );
            throw new BizException(BizErrorEnum.CONVERSATION_TARGET_QUERY_FAILED);
        }
    }

    /**
     * 转换用户信息映射.
     *
     * @param userInfoList 用户信息列表
     * @return 用户信息映射
     */
    private Map<BigInteger, ConversationUserInfoRespVO> toUserInfoMap(
            final List<ConversationUserInfo> userInfoList
    ) {
        if (isEmpty(userInfoList)) {
            return Map.of();
        }
        return userInfoList.stream()
                .collect(Collectors.toMap(
                        userInfo -> parseId(userInfo.getUserId()),
                        MessageGrpcConvertor.INSTANCE::toUserInfoRespVO,
                        (oldValue, newValue) -> oldValue
                ));
    }

    /**
     * 转换群聊信息映射.
     *
     * @param groupInfoList 群聊信息列表
     * @return 群聊信息映射
     */
    private Map<BigInteger, ConversationGroupInfoRespVO> toGroupInfoMap(
            final List<ConversationGroupInfo> groupInfoList
    ) {
        if (isEmpty(groupInfoList)) {
            return Map.of();
        }
        return groupInfoList.stream()
                .collect(Collectors.toMap(
                        groupInfo -> parseId(groupInfo.getGroupId()),
                        MessageGrpcConvertor.INSTANCE::toGroupInfoRespVO,
                        (oldValue, newValue) -> oldValue
                ));
    }

    /**
     * 转换id字符串列表.
     *
     * @param idList id列表
     * @return id字符串列表
     */
    private List<String> getStringList(final List<BigInteger> idList) {
        if (isEmpty(idList)) {
            return List.of();
        }
        return idList.stream()
                .filter(Objects::nonNull)
                .map(BigInteger::toString)
                .distinct()
                .toList();
    }

    /**
     * 判断列表是否为空.
     *
     * @param list 列表
     * @return 是否为空
     */
    private boolean isEmpty(final List<?> list) {
        return Objects.isNull(list) || list.isEmpty();
    }

    /**
     * 解析id.
     *
     * @param id id字符串
     * @return id
     */
    private BigInteger parseId(final String id) {
        return new BigInteger(id);
    }

    /**
     * 会话目标信息.
     */
    @Data
    @Builder
    public static class TargetInfo {

        /**
         * 用户信息映射.
         */
        private Map<BigInteger, ConversationUserInfoRespVO> userInfoMap;

        /**
         * 群聊信息映射.
         */
        private Map<BigInteger, ConversationGroupInfoRespVO> groupInfoMap;

        /**
         * 构建空会话目标信息.
         *
         * @return 空会话目标信息
         */
        public static TargetInfo empty() {
            return TargetInfo.builder()
                    .userInfoMap(Map.of())
                    .groupInfoMap(Map.of())
                    .build();
        }
    }
}
