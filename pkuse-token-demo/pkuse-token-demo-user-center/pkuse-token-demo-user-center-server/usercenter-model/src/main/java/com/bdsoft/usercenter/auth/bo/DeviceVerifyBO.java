package com.bdsoft.usercenter.auth.bo;

import java.io.Serializable;

/**
 * 新设备登录验证凭证（业务对象，存于 Redis）
 *
 * <p> 更换设备登录被拦截时下发（业务状态码 421），前端凭 verifyToken 发起短信验证码验证； </p>
 * <p> 验证通过后按此凭证中的设备指纹完成换绑并登录。仅 Service 层内部使用，不对外暴露。 </p>
 */
public class DeviceVerifyBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 登录账号 */
    private String username;

    /** 待绑定的新设备指纹 */
    private String deviceId;

    /** 设备类型：pc / mobile / mini */
    private String deviceType;

    /** 登录 IP */
    private String ip;

    /** User-Agent */
    private String userAgent;

    public DeviceVerifyBO() {
    }

    public DeviceVerifyBO(String userId, String username, String deviceId, String ip, String userAgent) {
        this.userId = userId;
        this.username = username;
        this.deviceId = deviceId;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public String toString() {
        return "DeviceVerifyBO{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", ip='" + ip + '\'' +
                ", userAgent='" + userAgent + '\'' +
                '}';
    }
}
