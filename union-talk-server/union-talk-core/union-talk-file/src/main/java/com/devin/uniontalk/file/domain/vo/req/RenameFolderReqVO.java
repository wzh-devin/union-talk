package com.devin.uniontalk.file.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 2026/06/30 18:40.
 *
 * <p>
 * 重命名目录请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "重命名目录请求参数")
public class RenameFolderReqVO {

    /**
     * 目录名称.
     */
    @NotBlank(message = "目录名称不能为空")
    @Size(max = 100, message = "目录名称长度超限")
    @Schema(description = "目录名称")
    private String name;
}
