package com.devin.uniontalk.message.domain.vo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.util.Date;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 2026/05/20 16:00.
 *
 * <p>
 * 会话分页查询请求参数
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Schema(description = "会话分页查询请求参数")
public class ConversationPageReqVO {

    /**
     * 游标更新时间.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "游标更新时间")
    private Date cursorValue;

    /**
     * 游标会话记录id.
     */
    @Schema(description = "游标会话记录id")
    private BigInteger cursorId;

    /**
     * 分页大小.
     */
    @Schema(description = "分页大小")
    private Integer pageSize;
}
