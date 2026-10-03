package com.devin.uniontalk.user.domain.model;

import com.devin.uniontalk.infrastructure.user.enums.GroupStatusEnum;
import com.devin.uniontalk.infrastructure.user.enums.UserStatusEnum;
import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/05/31 21:30.
 *
 * <p>
 * 会话目标信息模型
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
public class ConversationTargetInfo {

    /**
     * 用户信息列表.
     */
    private List<UserInfo> userInfoList;

    /**
     * 群聊信息列表.
     */
    private List<GroupInfo> groupInfoList;

    /**
     * 会话用户信息.
     */
    @Data
    @Builder
    public static class UserInfo {

        /**
         * 用户id.
         */
        private BigInteger userId;

        /**
         * 用户code.
         */
        private String code;

        /**
         * 用户名.
         */
        private String username;

        /**
         * 头像地址.
         */
        private String avatarUrl;

        /**
         * 用户状态.
         */
        private UserStatusEnum status;
    }

    /**
     * 会话群聊信息.
     */
    @Data
    @Builder
    public static class GroupInfo {

        /**
         * 群聊id.
         */
        private BigInteger groupId;

        /**
         * 群聊名称.
         */
        private String name;

        /**
         * 群聊头像.
         */
        private String avatarUrl;

        /**
         * 群聊描述.
         */
        private String description;

        /**
         * 群主id.
         */
        private BigInteger ownerId;

        /**
         * 当前成员数.
         */
        private Long memberCount;

        /**
         * 群聊状态.
         */
        private GroupStatusEnum status;
    }
}
