package com.devin.uniontalk.user.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 2026/05/17 15:10.
 *
 * <p>
 * 更新群聊信息请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "更新群聊信息请求参数")
public class UpdateGroupReqVO {

    /**
     * 群聊名称.
     */
    @Size(max = 100, message = "群聊名称长度超限")
    @Schema(description = "群聊名称")
    private String name;

    /**
     * 群聊头像.
     */
    @Schema(description = "群聊头像")
    private String avatarUrl;

    /**
     * 群聊描述.
     */
    @Size(max = 255, message = "群聊描述长度超限")
    @Schema(description = "群聊描述")
    private String description;

    /**
     * 人员限制.
     */
    @Min(value = 3, message = "人员限制不能少于3")
    @Max(value = 100, message = "人员限制不能超过100")
    @Schema(description = "人员限制")
    private Integer memberLimit;
}
