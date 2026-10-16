package com.bdsoft.usercenter.auth.dto;

/**
 * 新设备验证：发送验证码参数（数据传输对象）
 */
public class DeviceCodeDTO {

    /** 登录时（新设备）返回的验证凭证 */
    private String verifyToken;

    public String getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken) {
        this.verifyToken = verifyToken;
    }

    @Override
    public String toString() {
        return "DeviceCodeDTO{" +
                "verifyToken='" + verifyToken + '\'' +
                '}';
    }
}
