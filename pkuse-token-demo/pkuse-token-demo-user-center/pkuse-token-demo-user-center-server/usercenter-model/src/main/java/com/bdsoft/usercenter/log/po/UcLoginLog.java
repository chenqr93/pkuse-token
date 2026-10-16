package com.bdsoft.usercenter.log.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户中心-登录日志表（uc_login_log，按 create_time 按月分表）
 *
 * <p> 父表 uc_login_log 仅作分区路由：数据按 create_time 落到月分区 uc_login_log_yyyyMM， </p>
 * <p> 分区由 {@code LoginLogService} 在启动时与每日定时任务中自动创建。 </p>
 */
@TableName("uc_login_log")
public class UcLoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（32 位 UUID 字符串） */
    @TableId(type = IdType.ASSIGN_UUID)
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

    /** 发生时间（分区键） */
    private LocalDateTime createTime;

    /** 结果常量（供业务与页面展示使用） */
    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_FAIL = "fail";
    public static final String RESULT_LOCKED = "locked";
    public static final String RESULT_CAPTCHA_FAIL = "captcha_fail";
    public static final String RESULT_DEVICE_LOCK = "device_lock";
    public static final String RESULT_DEVICE_VERIFY = "device_verify";
    public static final String RESULT_DEVICE_REBIND = "device_rebind";
    public static final String RESULT_DISABLED = "disabled";
    public static final String RESULT_NO_USER = "no_user";

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
        return "UcLoginLog{" +
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
