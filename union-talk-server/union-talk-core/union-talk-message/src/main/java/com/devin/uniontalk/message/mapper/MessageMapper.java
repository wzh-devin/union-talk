package com.devin.uniontalk.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devin.uniontalk.message.domain.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 2026/05/20 15:20:24.
 *
 * <p>
 *  消息表(Message)Mapper层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
