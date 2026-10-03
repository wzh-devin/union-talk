package com.devin.uniontalk.user.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.util.List;
import lombok.Data;

/**
 * 2026/05/17 15:10.
 *
 * <p>
 * 创建群聊请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "创建群聊请求参数")
public class CreateGroupReqVO {

    /**
     * 群聊名称.
     */
    @NotBlank(message = "群聊名称不能为空")
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

    /**
     * 群聊人员id列表.
     */
    @NotEmpty(message = "群聊人员不能少于3人")
    @Size(min = 2, message = "群聊人员不能少于3人")
    @Schema(description = "群聊人员id列表")
    private List<BigInteger> uidList;
}
