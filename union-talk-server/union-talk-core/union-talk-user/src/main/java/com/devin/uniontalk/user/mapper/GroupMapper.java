package com.devin.uniontalk.user.mapper;

import com.devin.uniontalk.user.domain.entity.Group;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 2026/05/17 14:40:36.
 *
 * <p>
 *  群聊(Group)Mapper层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper
public interface GroupMapper extends BaseMapper<Group> {
    
}
    
