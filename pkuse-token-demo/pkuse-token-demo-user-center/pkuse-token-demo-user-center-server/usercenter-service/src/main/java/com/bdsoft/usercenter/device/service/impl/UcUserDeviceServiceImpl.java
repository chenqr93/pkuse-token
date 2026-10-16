package com.bdsoft.usercenter.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.usercenter.common.util.RequestInfoUtil;
import com.bdsoft.usercenter.device.po.UcUserDevice;
import com.bdsoft.usercenter.device.mapper.UcUserDeviceMapper;
import com.bdsoft.usercenter.device.service.UcUserDeviceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户设备 Service 实现
 */
@Service
public class UcUserDeviceServiceImpl extends ServiceImpl<UcUserDeviceMapper, UcUserDevice>
        implements UcUserDeviceService {

    @Override
    public List<UcUserDevice> listByUserId(String userId) {
        if (userId == null) {
            return List.of();
        }
        return this.list(new LambdaQueryWrapper<UcUserDevice>()
                .eq(UcUserDevice::getUserId, userId)
                .orderByDesc(UcUserDevice::getLastLoginTime));
    }

    @Override
    public long countByUserId(String userId) {
        if (userId == null) {
            return 0;
        }
        return this.count(new LambdaQueryWrapper<UcUserDevice>().eq(UcUserDevice::getUserId, userId));
    }

    @Override
    public Map<String, Integer> countByUserIds(List<String> userIds) {
        Map<String, Integer> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<UcUserDevice> devices = this.list(
                new LambdaQueryWrapper<UcUserDevice>().in(UcUserDevice::getUserId, userIds));
        for (UcUserDevice device : devices) {
            result.merge(device.getUserId(), 1, Integer::sum);
        }
        return result;
    }

    @Override
    public boolean isBound(String userId, String deviceId) {
        if (userId == null || deviceId == null || deviceId.isBlank()) {
            return false;
        }
        return this.count(new LambdaQueryWrapper<UcUserDevice>()
                .eq(UcUserDevice::getUserId, userId)
                .eq(UcUserDevice::getDeviceId, deviceId)) > 0;
    }

    @Override
    public void bind(String userId, String deviceId, String ip, String userAgent) {
        UcUserDevice device = new UcUserDevice();
        device.setUserId(userId);
        device.setDeviceId(deviceId);
        device.setUserAgent(userAgent);
        device.setOs(RequestInfoUtil.parseOs(userAgent));
        device.setBrowser(RequestInfoUtil.parseBrowser(userAgent));
        device.setLastLoginIp(ip);
        device.setFirstLoginTime(LocalDateTime.now());
        device.setLastLoginTime(LocalDateTime.now());
        this.save(device);
    }

    @Override
    public void touch(String userId, String deviceId, String ip, String userAgent) {
        UcUserDevice device = this.getOne(new LambdaQueryWrapper<UcUserDevice>()
                .eq(UcUserDevice::getUserId, userId)
                .eq(UcUserDevice::getDeviceId, deviceId));
        if (device == null) {
            return;
        }
        device.setLastLoginIp(ip);
        device.setUserAgent(userAgent);
        device.setOs(RequestInfoUtil.parseOs(userAgent));
        device.setBrowser(RequestInfoUtil.parseBrowser(userAgent));
        device.setLastLoginTime(LocalDateTime.now());
        this.updateById(device);
    }

    @Override
    public void unbind(String userId, String deviceId) {
        if (userId == null || deviceId == null || deviceId.isBlank()) {
            return;
        }
        this.remove(new LambdaQueryWrapper<UcUserDevice>()
                .eq(UcUserDevice::getUserId, userId)
                .eq(UcUserDevice::getDeviceId, deviceId));
    }

    @Override
    public void replaceDevices(String userId, String deviceId, String ip, String userAgent) {
        // 解绑原设备，绑定新设备（新设备验证通过后换绑）
        this.removeByUserId(userId);
        this.bind(userId, deviceId, ip, userAgent);
    }

    @Override
    public void removeByUserId(String userId) {
        if (userId == null) {
            return;
        }
        this.remove(new LambdaQueryWrapper<UcUserDevice>().eq(UcUserDevice::getUserId, userId));
    }

}
