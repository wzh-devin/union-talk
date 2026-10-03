package com.devin.uniontalk.file.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import lombok.Data;

/**
 * 2026/06/30 18:40.
 *
 * <p>
 * 创建目录请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "创建目录请求参数")
public class CreateFolderReqVO {

    /**
     * 会话id.
     */
    @NotNull(message = "会话id不能为空")
    @Schema(description = "会话id")
    private BigInteger conversationId;

    /**
     * 父级目录id.
     */
    @Schema(description = "父级目录id，不传时使用当前用户成员根目录，群主传0时使用会话根目录")
    private BigInteger parentId;

    /**
     * 目录名称.
     */
    @NotBlank(message = "目录名称不能为空")
    @Size(max = 100, message = "目录名称长度超限")
    @Schema(description = "目录名称")
    private String name;
}
