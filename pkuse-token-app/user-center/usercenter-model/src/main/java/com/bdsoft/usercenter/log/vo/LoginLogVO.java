package com.bdsoft.usercenter.log.vo;

import com.bdsoft.usercenter.log.po.UcLoginLog;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志视图对象（Controller 返回给前端的响应对象）
 */
public class LoginLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（序列化为字符串，避免前端 JS 精度丢失） */
    private String id;

    /** 登录账号 */
    private String username;

    /** 用户 ID */
    private String userId;

    /** 类型：login=登录 / logout=注销 */
    private String loginType;

    /** 结果：success / fail / locked / captcha_fail / device_lock / disabled / no_user */
    private String result;

    /** 说明信息 */
    private String msg;

    /** 客户端 IP */
    private String ip;

    /** User-Agent */
    private String userAgent;

    /** 操作系统 */
    private String os;

    /** 浏览器 */
    private String browser;

    /** 设备指纹 */
    private String deviceId;

    /** 发生时间 */
    private LocalDateTime createTime;

    /** 由实体转换为视图对象 */
    public static LoginLogVO from(UcLoginLog log) {
        if (log == null) {
            return null;
        }
        LoginLogVO vo = new LoginLogVO();
        vo.setId(log.getId());
        vo.setUsername(log.getUsername());
        vo.setUserId(log.getUserId());
        vo.setLoginType(log.getLoginType());
        vo.setResult(log.getResult());
        vo.setMsg(log.getMsg());
        vo.setIp(log.getIp());
        vo.setUserAgent(log.getUserAgent());
        vo.setOs(log.getOs());
        vo.setBrowser(log.getBrowser());
        vo.setDeviceId(log.getDeviceId());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "LoginLogVO{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", userId='" + userId + '\'' +
                ", loginType='" + loginType + '\'' +
                ", result='" + result + '\'' +
                ", msg='" + msg + '\'' +
                ", ip='" + ip + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", os='" + os + '\'' +
                ", browser='" + browser + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
