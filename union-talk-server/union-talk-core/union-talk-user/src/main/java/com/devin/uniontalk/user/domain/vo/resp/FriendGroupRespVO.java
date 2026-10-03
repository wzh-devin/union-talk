package com.devin.uniontalk.user.domain.vo.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.math.BigInteger;

/**
 * 2026/05/17 15:10.
 *
 * <p>
 * 好友分组响应参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "好友分组响应参数")
public class FriendGroupRespVO {

    @Schema(description = "分组id")
    private BigInteger id;

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "分组排序")
    private Integer sortOrder;
}
