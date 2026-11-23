package com.bdsoft.usercenter.device.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdsoft.usercenter.common.pojo.AbstractAuditPO;

import java.time.LocalDateTime;

/**
 * 用户中心-用户设备表（uc_user_device）
 *
 * <p> 设备锁：开启后账号仅在已绑定设备上允许登录；设备标识来自客户端上报的「设备指纹」。 </p>
 */
@TableName("uc_user_device")
public class UcUserDevice extends AbstractAuditPO {

    private static final long serialVersionUID = 1L;

    /** 主键（32 位 UUID 字符串） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户 ID */
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
        return "UcUserDevice{" + super.toString() +
                ", id='" + id + '\'' +
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
