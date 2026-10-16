package com.bdsoft.usercenter.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录安全策略配置（对应 application.yml 中的 user-center.login.*）
 *
 * <p> 注意：失败计数窗口、锁定阈值、验证码策略、设备验证开关、默认密码、密码复杂度等 </p>
 * <p> 已迁移到「规则配置」模块（uc_config，仅系统管理员可在线维护），此处仅保留部署级参数。 </p>
 */
@Component
@ConfigurationProperties(prefix = "user-center.login")
public class LoginSecurityProperties {

    /** 允许绑定的设备数量上限 */
    private int maxDevices = 1;

    /** 内置管理员账号是否豁免设备锁（避免运维被锁在门外，生产可按需开启） */
    private boolean deviceLockExemptAdmin = false;

    /** 新设备验证码有效期（分钟） */
    private int deviceVerifyMinutes = 5;

    /** 新设备登录凭证（verifyToken）有效期（分钟） */
    private int deviceVerifyTokenMinutes = 5;

    /**
     * 演示模式：新设备验证码不真实发送短信，改为在响应中返回（同时打印到日志），便于无短信通道时演示；
     * 生产环境必须为 false。
     */
    private boolean deviceVerifyShowCode = false;

    public int getMaxDevices() {
        return maxDevices;
    }

    public void setMaxDevices(int maxDevices) {
        this.maxDevices = maxDevices;
    }

    public boolean isDeviceLockExemptAdmin() {
        return deviceLockExemptAdmin;
    }

    public void setDeviceLockExemptAdmin(boolean deviceLockExemptAdmin) {
        this.deviceLockExemptAdmin = deviceLockExemptAdmin;
    }

    public int getDeviceVerifyMinutes() {
        return deviceVerifyMinutes;
    }

    public void setDeviceVerifyMinutes(int deviceVerifyMinutes) {
        this.deviceVerifyMinutes = deviceVerifyMinutes;
    }

    public int getDeviceVerifyTokenMinutes() {
        return deviceVerifyTokenMinutes;
    }

    public void setDeviceVerifyTokenMinutes(int deviceVerifyTokenMinutes) {
        this.deviceVerifyTokenMinutes = deviceVerifyTokenMinutes;
    }

    public boolean isDeviceVerifyShowCode() {
        return deviceVerifyShowCode;
    }

    public void setDeviceVerifyShowCode(boolean deviceVerifyShowCode) {
        this.deviceVerifyShowCode = deviceVerifyShowCode;
    }

}
