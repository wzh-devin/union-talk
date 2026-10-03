package com.devin.uniontalk.websocket.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigInteger;
import java.util.List;
import lombok.Data;

/**
 * 2026/05/19 00:00.
 *
 * <p>
 * 批量查询在线状态请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "批量查询在线状态请求参数")
public class OnlineCheckReqVO {

    /**
     * 用户id列表.
     */
    @NotEmpty(message = "用户id列表不能为空")
    @Schema(description = "用户id列表")
    private List<BigInteger> userIdList;
}
