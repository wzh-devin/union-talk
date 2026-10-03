package com.devin.uniontalk.user.dao;

import com.devin.uniontalk.user.domain.entity.UserSetting;
import com.devin.uniontalk.user.mapper.UserSettingMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 2026/05/12 22:01:41.
 *
 * <p>
 *  用户设置(UserSetting)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingDao extends ServiceImpl<UserSettingMapper, UserSetting> {

}
