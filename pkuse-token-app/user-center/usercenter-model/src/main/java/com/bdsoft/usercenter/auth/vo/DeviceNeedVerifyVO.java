package com.bdsoft.usercenter.auth.vo;

import java.io.Serializable;

/**
 * 新设备登录验证提示视图对象
 *
 * <p> 登录被设备锁拦截（业务状态码 421）时随异常返回给前端，前端据此展示验证码验证界面。 </p>
 */
public class DeviceNeedVerifyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否需要新设备验证（返回时置为 true，前端据此识别新设备验证流程） */
    private Boolean needVerify;

    /** 验证凭证：后续发送/校验验证码时回传 */
    private String verifyToken;

    /** 脱敏手机号（验证码发送目标） */
    private String phone;

    public Boolean getNeedVerify() {
        return needVerify;
    }

    public void setNeedVerify(Boolean needVerify) {
        this.needVerify = needVerify;
    }

    public String getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken) {
        this.verifyToken = verifyToken;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "DeviceNeedVerifyVO{" +
                "needVerify=" + needVerify +
                ", verifyToken='" + verifyToken + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
