package com.devin.uniontalk.message.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigInteger;
import java.util.Date;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 2026/05/20 16:00.
 *
 * <p>
 * 消息分页查询请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "消息分页查询请求参数")
public class MessagePageReqVO {

    /**
     * 会话id.
     */
    @NotNull(message = "会话id不能为空")
    @Schema(description = "会话id")
    private BigInteger conversationId;

    /**
     * 游标创建时间.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "游标创建时间")
    private Date cursorValue;

    /**
     * 游标消息id.
     */
    @Schema(description = "游标消息id")
    private BigInteger cursorId;

    /**
     * 分页大小.
     */
    @Schema(description = "分页大小")
    private Integer pageSize;
}
