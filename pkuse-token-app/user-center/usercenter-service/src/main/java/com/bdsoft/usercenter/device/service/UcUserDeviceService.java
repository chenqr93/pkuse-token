package com.bdsoft.usercenter.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.usercenter.device.po.UcUserDevice;

import java.util.List;
import java.util.Map;

/**
 * 用户设备 Service（设备锁 / 设备指纹）
 *
 * <p> 表中记录账号已绑定的设备（设备指纹维度）；设备锁开启时，未绑定设备将被拒绝登录， </p>
 * <p> 由管理员在「用户管理 - 设备管理」中在线解绑。 </p>
 */
public interface UcUserDeviceService extends IService<UcUserDevice> {

    /** 查询用户已绑定设备（最近登录时间倒序） */
    List<UcUserDevice> listByUserId(String userId);

    /** 用户已绑定设备数 */
    long countByUserId(String userId);

    /** 批量统计：用户 ID -> 设备数（供用户列表展示） */
    Map<String, Integer> countByUserIds(List<String> userIds);

    /** 判断设备是否已绑定到该用户 */
    boolean isBound(String userId, String deviceId);

    /** 绑定设备（首次在该设备登录时调用） */
    void bind(String userId, String deviceId, String ip, String userAgent);

    /** 更新设备的最近登录信息 */
    void touch(String userId, String deviceId, String ip, String userAgent);

    /** 解绑设备（管理员在线解绑；解绑后该设备需重新绑定） */
    void unbind(String userId, String deviceId);

    /** 换绑设备：解绑该用户原有全部设备，并绑定新设备（新设备验证通过后调用） */
    void replaceDevices(String userId, String deviceId, String ip, String userAgent);

    /** 用户删除时清理其设备记录 */
    void removeByUserId(String userId);

}
