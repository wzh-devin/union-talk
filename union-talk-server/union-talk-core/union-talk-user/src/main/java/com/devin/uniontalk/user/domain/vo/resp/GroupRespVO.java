package com.devin.uniontalk.user.domain.vo.resp;

import com.devin.uniontalk.infrastructure.user.enums.GroupStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigInteger;

/**
 * 2026/05/17 15:10.
 *
 * <p>
 * 群聊信息响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "群聊信息响应参数")
public class GroupRespVO {

    @Schema(description = "群聊id")
    private BigInteger id;

    @Schema(description = "群聊名称")
    private String name;

    @Schema(description = "群聊头像")
    private String avatarUrl;

    @Schema(description = "群聊描述")
    private String description;

    @Schema(description = "群主id")
    private BigInteger ownerId;

    @Schema(description = "人员限制")
    private Integer memberLimit;

    @Schema(description = "当前成员数")
    private Long memberCount;

    @Schema(description = "群聊状态")
    private GroupStatusEnum status;
}
