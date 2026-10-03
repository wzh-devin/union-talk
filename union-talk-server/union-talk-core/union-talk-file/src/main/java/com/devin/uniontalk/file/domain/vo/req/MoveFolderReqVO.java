package com.devin.uniontalk.file.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigInteger;
import lombok.Data;

/**
 * 2026/06/30 18:40.
 *
 * <p>
 * 移动目录请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "移动目录请求参数")
public class MoveFolderReqVO {

    /**
     * 目标父级目录id.
     */
    @NotNull(message = "目标父级目录id不能为空")
    @Schema(description = "目标父级目录id")
    private BigInteger targetParentId;
}
