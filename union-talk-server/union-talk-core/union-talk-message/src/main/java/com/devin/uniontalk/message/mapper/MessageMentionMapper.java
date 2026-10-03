package com.devin.uniontalk.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devin.uniontalk.message.domain.entity.MessageMention;
import org.apache.ibatis.annotations.Mapper;

/**
 * 2026/08/06 23:05.
 *
 * <p>
 * 消息提及Mapper层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper
public interface MessageMentionMapper extends BaseMapper<MessageMention> {
}
