package com.devin.uniontalk.user.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigInteger;

/**
 * 2026/05/17 15:10.
 *
 * <p>
 * 修改好友备注请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "修改好友备注请求参数")
public class UpdateRemarkReqVO {

    @NotNull(message = "目标用户id不能为空")
    @Schema(description = "目标用户id")
    private BigInteger targetId;

    @NotNull(message = "备注不能为空")
    @Size(max = 64, message = "备注长度超限")
    @Schema(description = "备注")
    private String remark;
}
