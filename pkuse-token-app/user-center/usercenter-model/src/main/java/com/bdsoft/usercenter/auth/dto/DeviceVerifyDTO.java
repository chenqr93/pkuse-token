package com.bdsoft.usercenter.auth.dto;

/**
 * 新设备验证：校验验证码参数（数据传输对象）
 */
public class DeviceVerifyDTO {

    /** 登录时（新设备）返回的验证凭证 */
    private String verifyToken;

    /** 短信验证码 */
    private String code;

    public String getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken) {
        this.verifyToken = verifyToken;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "DeviceVerifyDTO{" +
                "verifyToken='" + verifyToken + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}
