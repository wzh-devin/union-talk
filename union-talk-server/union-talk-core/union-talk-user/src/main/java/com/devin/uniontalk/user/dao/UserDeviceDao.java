package com.devin.uniontalk.user.dao;

import com.devin.uniontalk.base.utils.IdGenerator;
import com.devin.uniontalk.infrastructure.user.enums.DeviceTypeEnum;
import com.devin.uniontalk.user.domain.entity.UserDevice;
import com.devin.uniontalk.user.mapper.UserDeviceMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigInteger;

/**
 * 2026/05/12 22:01:39.
 *
 * <p>
 *  用户登录设备(UserDevice)Dao层
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceDao extends ServiceImpl<UserDeviceMapper, UserDevice> {

    /**
     * 记录登录设备，已存在则更新活跃时间，不存在则新增.
     *
     * @param userId    用户ID
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     * @param deviceType 设备类型
     * @param deviceName 设备名称
     */
    public void recordDevice(
            final BigInteger userId,
            final String ipAddress,
            final String userAgent,
            final DeviceTypeEnum deviceType,
            final String deviceName
    ) {
        // 根据用户ID和IP地址查询是否已有设备记录（ip_address 为 INET 类型，需显式转换）
        UserDevice existDevice = lambdaQuery()
                .eq(UserDevice::getUserId, userId)
                .apply("ip_address = {0}::inet", ipAddress)
                .one();

        if (existDevice != null) {
            // 已存在，更新活跃时间和设备信息
            existDevice.setUserAgent(userAgent);
            existDevice.setDeviceType(deviceType.name());
            existDevice.setDeviceName(deviceName);
            existDevice.recordActive();
            updateById(existDevice);
        } else {
            // 不存在，新增设备记录
            UserDevice device = new UserDevice();
            device.setId(IdGenerator.nextIdBigInteger());
            device.setUserId(userId);
            device.setIpAddress(ipAddress);
            device.setUserAgent(userAgent);
            device.setDeviceType(deviceType.name());
            device.setDeviceName(deviceName);
            device.init();
            save(device);
        }
    }
}
