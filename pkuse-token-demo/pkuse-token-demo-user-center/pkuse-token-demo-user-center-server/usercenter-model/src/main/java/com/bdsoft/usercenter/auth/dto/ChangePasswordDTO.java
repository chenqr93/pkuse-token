package com.bdsoft.usercenter.auth.dto;

import java.io.Serializable;

/**
 * 修改密码参数（数据传输对象：原密码 + 新密码，支持 SM2 密文）
 */
public class ChangePasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 原密码（明文或 SM2 密文） */
    private String oldPassword;

    /** 新密码（明文或 SM2 密文） */
    private String newPassword;

    /** 是否为国密 SM2 密文（前端统一加密传输时为 true） */
    private Boolean encrypted;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    @Override
    public String toString() {
        return "ChangePasswordDTO{" +
                "encrypted=" + encrypted +
                '}';
    }
}
