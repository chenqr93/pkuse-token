package com.bdsoft.usercenter.auth.dto;

import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;

/**
 * 登录参数（数据传输对象）
 */
public class LoginDTO {

    /** 登录账号 */
    private String username;

    /** 密码（明文；国密模式下可为 SM2 密文，见 encrypted） */
    private String password;

    /** 密码是否为 SM2 密文（前端使用 /api/auth/public-key 的公钥加密） */
    private Boolean encrypted;

    /** 设备指纹（前端按浏览器环境生成，用于设备锁） */
    private String deviceId;

    /** 设备类型：pc / mobile / mini（小程序）；未上报时服务端按 User-Agent 推断 */
    private String deviceType;

    /** 滑块验证码 id（需要验证码时由 /api/auth/captcha 返回） */
    private String captchaId;

    /** 滑块验证码轨迹与位置信息 */
    private ImageCaptchaTrack captchaTrack;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
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

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public ImageCaptchaTrack getCaptchaTrack() {
        return captchaTrack;
    }

    public void setCaptchaTrack(ImageCaptchaTrack captchaTrack) {
        this.captchaTrack = captchaTrack;
    }

    @Override
    public String toString() {
        return "LoginDTO{" +
                "username='" + username + '\'' +
                ", encrypted=" + encrypted +
                ", deviceId='" + deviceId + '\'' +
                ", captchaId='" + captchaId + '\'' +
                ", captchaTrack=" + captchaTrack +
                '}';
    }
}
