package com.bdsoft.usercenter.auth.vo;

import com.bdsoft.usercenter.common.config.SystemConfig;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 密码规则视图对象（登录/改密页面提示用）
 *
 * <p> {@code defaultPassword} 仅管理端（/api/config/password-rule）返回，登录用户可见的
 * /api/auth/password-rule 不返回该字段，为空时序列化时省略。 </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PasswordRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否开启密码复杂度校验 */
    private Boolean complexityEnabled;

    /** 密码最小长度 */
    private Integer minLength;

    /** 是否必须包含大写字母 */
    private Boolean requireUppercase;

    /** 是否必须包含小写字母 */
    private Boolean requireLowercase;

    /** 是否必须包含数字 */
    private Boolean requireDigit;

    /** 是否必须包含特殊字符 */
    private Boolean requireSpecial;

    /** 规则文案，如：密码长度不少于 8 位，须包含大写字母、小写字母、数字 */
    private String text;

    /** 系统默认密码（仅管理端返回，登录用户可见接口不下发） */
    private String defaultPassword;

    /** 由规则配置快照转换为视图对象 */
    public static PasswordRuleVO from(SystemConfig config) {
        PasswordRuleVO vo = new PasswordRuleVO();
        vo.setComplexityEnabled(config.isPasswordComplexityEnabled());
        vo.setMinLength(config.getPasswordMinLength());
        vo.setRequireUppercase(config.isPasswordRequireUppercase());
        vo.setRequireLowercase(config.isPasswordRequireLowercase());
        vo.setRequireDigit(config.isPasswordRequireDigit());
        vo.setRequireSpecial(config.isPasswordRequireSpecial());
        vo.setText(config.passwordRuleText());
        return vo;
    }

    public Boolean getComplexityEnabled() {
        return complexityEnabled;
    }

    public void setComplexityEnabled(Boolean complexityEnabled) {
        this.complexityEnabled = complexityEnabled;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Boolean getRequireUppercase() {
        return requireUppercase;
    }

    public void setRequireUppercase(Boolean requireUppercase) {
        this.requireUppercase = requireUppercase;
    }

    public Boolean getRequireLowercase() {
        return requireLowercase;
    }

    public void setRequireLowercase(Boolean requireLowercase) {
        this.requireLowercase = requireLowercase;
    }

    public Boolean getRequireDigit() {
        return requireDigit;
    }

    public void setRequireDigit(Boolean requireDigit) {
        this.requireDigit = requireDigit;
    }

    public Boolean getRequireSpecial() {
        return requireSpecial;
    }

    public void setRequireSpecial(Boolean requireSpecial) {
        this.requireSpecial = requireSpecial;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDefaultPassword() {
        return defaultPassword;
    }

    public void setDefaultPassword(String defaultPassword) {
        this.defaultPassword = defaultPassword;
    }

    @Override
    public String toString() {
        return "PasswordRuleVO{" +
                "complexityEnabled=" + complexityEnabled +
                ", minLength=" + minLength +
                ", requireUppercase=" + requireUppercase +
                ", requireLowercase=" + requireLowercase +
                ", requireDigit=" + requireDigit +
                ", requireSpecial=" + requireSpecial +
                ", text='" + text + '\'' +
                '}';
    }
}
