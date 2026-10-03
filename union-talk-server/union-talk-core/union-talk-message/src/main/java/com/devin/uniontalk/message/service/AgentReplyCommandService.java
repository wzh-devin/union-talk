package com.devin.uniontalk.message.service;

import com.devin.uniontalk.message.domain.command.AgentReplyCommand;
import com.devin.uniontalk.message.domain.model.AgentReplyResult;

/**
 * 2026/07/28 23:30.
 *
 * <p>
 * Agent回复写回命令服务
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public interface AgentReplyCommandService {

    /**
     * 创建Agent正式回复消息.
     *
     * @param command Agent回复写回命令
     * @return Agent回复写回结果
     */
    AgentReplyResult createAgentReply(AgentReplyCommand command);
}
