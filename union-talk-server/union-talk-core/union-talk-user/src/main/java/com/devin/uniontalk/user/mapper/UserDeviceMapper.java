package com.devin.uniontalk.user.mapper;

import com.devin.uniontalk.user.domain.entity.UserDevice;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 2026/05/12 22:01:39.
 *
 * <p>
 *  用户登录设备(UserDevice)Mapper层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Mapper
public interface UserDeviceMapper extends BaseMapper<UserDevice> {
    
}
    
