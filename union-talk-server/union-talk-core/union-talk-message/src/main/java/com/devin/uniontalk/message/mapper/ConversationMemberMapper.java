package com.devin.uniontalk.message.mapper;

import com.devin.uniontalk.message.domain.entity.ConversationMember;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 2026/05/20 15:20:24.
 *
 * <p>
 *  私聊会话成员(ConversationMember)Mapper层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper
public interface ConversationMemberMapper extends BaseMapper<ConversationMember> {
    
}
    
