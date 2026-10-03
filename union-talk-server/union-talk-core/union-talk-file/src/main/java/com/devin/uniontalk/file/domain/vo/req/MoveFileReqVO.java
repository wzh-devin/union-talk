package com.devin.uniontalk.file.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigInteger;
import lombok.Data;

/**
 * 2026/06/30 18:40.
 *
 * <p>
 * 移动文件请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "移动文件请求参数")
public class MoveFileReqVO {

    /**
     * 目标文件夹id.
     */
    @NotNull(message = "目标文件夹id不能为空")
    @Schema(description = "目标文件夹id")
    private BigInteger targetFolderId;
}
