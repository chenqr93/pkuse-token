package com.bdsoft.usercenter.user.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdsoft.usercenter.common.pojo.AbstractAuditPO;

import java.time.LocalDateTime;

/**
 * 用户中心-用户表（uc_user）
 */
@TableName("uc_user")
public class UcUser extends AbstractAuditPO {

    private static final long serialVersionUID = 1L;

    /** 角色：系统管理员 */
    public static final String ROLE_ADMIN = "admin";

    /** 角色：普通用户 */
    public static final String ROLE_USER = "user";

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 1;

    /** 状态：禁用 */
    public static final int STATUS_DISABLED = 0;

    /** 「需修改密码」标记：下次登录需强制修改 */
    public static final int MUST_CHANGE_PASSWORD_YES = 1;

    /** 「需修改密码」标记：无需修改 */
    public static final int MUST_CHANGE_PASSWORD_NO = 0;

    /** 主键（32 位 UUID 字符串） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 登录账号 */
    private String username;

    /** 密码（SM3(salt + ":" + 明文)；历史数据为 MD5(明文)） */
    private String password;

    /** 密码盐（国密 SM3 摘要使用；历史 MD5 数据此列为空，登录成功后自动升级） */
    private String passwordSalt;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 角色：admin=管理员 / user=普通用户 */
    private String role;

    /** 状态：0=禁用 / 1=启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 连续登录失败次数 */
    private Integer loginFailCount;

    /** 锁定截止时间（为空表示未锁定） */
    private LocalDateTime lockedUntil;

    /** 锁定原因 */
    private String lockReason;

    /**
     * 是否需要修改密码：1=下次登录需强制修改
     *
     * <p> 由「规则配置 - 首次登录强制修改密码」控制：开启后，新增用户与被重置密码的用户置位； </p>
     * <p> 用户修改密码成功后清零。 </p>
     */
    private Integer mustChangePassword;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public Integer getLoginFailCount() {
        return loginFailCount;
    }

    public void setLoginFailCount(Integer loginFailCount) {
        this.loginFailCount = loginFailCount;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public String getLockReason() {
        return lockReason;
    }

    public void setLockReason(String lockReason) {
        this.lockReason = lockReason;
    }

    public Integer getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Integer mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    @Override
    public String toString() {
        return "UcUser{" + super.toString() +
                ", id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                ", status=" + status +
                ", remark='" + remark + '\'' +
                ", lastLoginTime=" + lastLoginTime +
                ", loginFailCount=" + loginFailCount +
                ", lockedUntil=" + lockedUntil +
                ", lockReason='" + lockReason + '\'' +
                ", mustChangePassword=" + mustChangePassword +
                '}';
    }
}
