package com.bdsoft.usercenter.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 新设备验证：验证码发送结果视图对象
 *
 * <p> {@code demoCode} 仅在演示模式（device-verify-show-code=true）返回，为空时序列化时省略。 </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceCodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 脱敏手机号（138****0001） */
    private String phone;

    /** 验证码有效期（分钟） */
    private Integer expireMinutes;

    /** 演示模式下的验证码（无短信通道时直接返回展示，生产环境必须关闭） */
    private String demoCode;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(Integer expireMinutes) {
        this.expireMinutes = expireMinutes;
    }

    public String getDemoCode() {
        return demoCode;
    }

    public void setDemoCode(String demoCode) {
        this.demoCode = demoCode;
    }

    @Override
    public String toString() {
        return "DeviceCodeVO{" +
                "phone='" + phone + '\'' +
                ", expireMinutes=" + expireMinutes +
                '}';
    }
}
