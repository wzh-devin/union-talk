package com.devin.uniontalk.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devin.uniontalk.message.domain.entity.MessageAgentExtension;
import java.math.BigInteger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 2026/07/28 22:15.
 *
 * <p>
 * AI消息扩展Mapper层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper
public interface MessageAgentExtensionMapper extends BaseMapper<MessageAgentExtension> {

    /**
     * 获取Agent回复事务锁.
     *
     * @param agentRunId Agent运行id
     * @return 锁定结果
     */
    Integer lockAgentRun(@Param("agentRunId") BigInteger agentRunId);

}
