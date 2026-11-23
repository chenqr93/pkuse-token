package com.bdsoft.usercenter.auth.bo;

/**
 * 新设备验证码发送结果（业务对象，Service 层内部使用）
 *
 * <p> 由 Controller 转换为 {@code DeviceCodeVO} 返回前端。 </p>
 */
public class DeviceCodeBO {

    /** 脱敏手机号（138****0001） */
    private final String phone;

    /** 验证码有效期（分钟） */
    private final Integer expireMinutes;

    /** 演示模式下的验证码（无短信通道时返回展示，生产环境为 null） */
    private final String demoCode;

    public DeviceCodeBO(String phone, Integer expireMinutes, String demoCode) {
        this.phone = phone;
        this.expireMinutes = expireMinutes;
        this.demoCode = demoCode;
    }

    public String getPhone() {
        return phone;
    }

    public Integer getExpireMinutes() {
        return expireMinutes;
    }

    public String getDemoCode() {
        return demoCode;
    }

    @Override
    public String toString() {
        return "DeviceCodeBO{" +
                "phone='" + phone + '\'' +
                ", expireMinutes=" + expireMinutes +
                '}';
    }
}
