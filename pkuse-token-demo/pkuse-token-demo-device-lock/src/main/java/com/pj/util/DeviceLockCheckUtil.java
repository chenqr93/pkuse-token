package com.pj.util;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;

/**
 * 设备锁操作工具类
 * @author click33
 * @since 2025/3/5
 */
public class DeviceLockCheckUtil {

    /**
     * 保存设备id与用户id的映射关系
     * @param deviceId /
     * @param userId /
     */
    public static void setDeviceIdToUserId(String deviceId, long userId) {
        if(PkuseFoxUtil.isEmpty(deviceId) || PkuseFoxUtil.isEmpty(userId)) {
            throw new RuntimeException("设备id或用户id不能为空");
        }
        PkuseManager.getSaTokenDao().set(saveKeyPrefix() + deviceId, String.valueOf(userId), 1200);
    }

    /**
     * 返回设备id绑定的用户id
     * @param deviceId /
     */
    public static long getUserIdByDeviceId(String deviceId) {
        String userIdStr = PkuseManager.getSaTokenDao().get(saveKeyPrefix() + deviceId);
        if(userIdStr == null) {
            throw new RuntimeException("此设备id目前未绑定任何用户");
        }
        return Long.parseLong(userIdStr);
    }

    // 返回数据保存时使用的前缀
    public static Object saveKeyPrefix() {
        return PkuseManager.getConfig().getTokenName() + ":device-to-userid:";
    }

}