package com.bdsoft.usercenter.device.vo;

import com.bdsoft.usercenter.device.po.UcUserDevice;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户设备视图对象（设备锁 / 设备指纹，Controller 返回给前端的响应对象）
 */
public class DeviceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（序列化为字符串，避免前端 JS 精度丢失） */
    private String id;

    /** 所属用户 ID */
    private String userId;

    /** 设备指纹（客户端按浏览器环境生成） */
    private String deviceId;

    /** 最近一次登录的 User-Agent */
    private String userAgent;

    /** 操作系统 */
    private String os;

    /** 浏览器 */
    private String browser;

    /** 最近一次登录 IP */
    private String lastLoginIp;

    /** 首次登录时间（设备绑定时间） */
    private LocalDateTime firstLoginTime;

    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;

    /** 由实体转换为视图对象 */
    public static DeviceVO from(UcUserDevice device) {
        if (device == null) {
            return null;
        }
        DeviceVO vo = new DeviceVO();
        vo.setId(device.getId());
        vo.setUserId(device.getUserId());
        vo.setDeviceId(device.getDeviceId());
        vo.setUserAgent(device.getUserAgent());
        vo.setOs(device.getOs());
        vo.setBrowser(device.getBrowser());
        vo.setLastLoginIp(device.getLastLoginIp());
        vo.setFirstLoginTime(device.getFirstLoginTime());
        vo.setLastLoginTime(device.getLastLoginTime());
        return vo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public LocalDateTime getFirstLoginTime() {
        return firstLoginTime;
    }

    public void setFirstLoginTime(LocalDateTime firstLoginTime) {
        this.firstLoginTime = firstLoginTime;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    @Override
    public String toString() {
        return "DeviceVO{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", os='" + os + '\'' +
                ", browser='" + browser + '\'' +
                ", lastLoginIp='" + lastLoginIp + '\'' +
                ", firstLoginTime=" + firstLoginTime +
                ", lastLoginTime=" + lastLoginTime +
                '}';
    }
}
