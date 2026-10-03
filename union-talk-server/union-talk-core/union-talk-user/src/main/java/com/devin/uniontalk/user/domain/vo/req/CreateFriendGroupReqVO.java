package com.devin.uniontalk.user.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 2026/05/17 15:10.
 *
 * <p>
 * 创建好友分组请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "创建好友分组请求参数")
public class CreateFriendGroupReqVO {

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 32, message = "分组名称长度超限")
    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "分组排序")
    private Integer sortOrder;
}
