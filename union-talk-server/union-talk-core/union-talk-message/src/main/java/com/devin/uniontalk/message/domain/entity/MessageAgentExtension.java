package com.devin.uniontalk.message.domain.entity;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.devin.uniontalk.datasource.handler.JsonbStringTypeHandler;
import com.devin.uniontalk.message.domain.command.AgentReplyCommand;
import com.devin.uniontalk.message.domain.model.AgentCitation;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 2026/07/28 22:12.
 *
 * <p>
 * AI消息扩展实体
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@TableName(value = "ut_message_agent_extension", autoResultMap = true)
public class MessageAgentExtension implements Serializable {

    /**
     * 序列化版本号.
     */
    @Serial
    private static final long serialVersionUID = -6706877160092771790L;

    /**
     * AI回复消息id.
     */
    @TableId(value = "message_id", type = IdType.INPUT)
    private BigInteger messageId;

    /**
     * Agent运行id.
     */
    @TableField("agent_run_id")
    private BigInteger agentRunId;

    /**
     * 触发消息id.
     */
    @TableField("trigger_message_id")
    private BigInteger triggerMessageId;

    /**
     * 稳定Agent定义id.
     */
    @TableField("agent_id")
    private BigInteger agentId;

    /**
     * 模型标识.
     */
    @TableField("model_id")
    private String modelId;

    /**
     * 引用信息JSON.
     */
    @TableField(value = "citations_json", typeHandler = JsonbStringTypeHandler.class)
    private String citationsJson;

    /**
     * 创建时间.
     */
    @TableField("created_at")
    private Date createdAt;

    /**
     * 根据回复命令初始化扩展信息.
     *
     * @param messageId AI回复消息id
     * @param command   Agent回复命令
     */
    public void init(final BigInteger messageId, final AgentReplyCommand command) {
        this.messageId = messageId;
        this.agentRunId = command.getRunId();
        this.triggerMessageId = command.getTriggerMessageId();
        this.agentId = command.getAgentId();
        this.modelId = command.getModelId();
        this.citationsJson = JSON.toJSONString(
                command.getCitationList() == null ? List.of() : command.getCitationList()
        );
        this.createdAt = new Date();
    }

    /**
     * 解析引用信息列表.
     *
     * @return 引用信息列表
     */
    public List<AgentCitation> getCitationList() {
        if (citationsJson == null || citationsJson.isBlank()) {
            return List.of();
        }
        return JSON.parseArray(citationsJson, AgentCitation.class);
    }
}
