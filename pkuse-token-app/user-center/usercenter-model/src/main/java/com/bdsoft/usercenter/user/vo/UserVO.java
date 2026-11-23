package com.bdsoft.usercenter.user.vo;

import com.bdsoft.usercenter.user.po.UcUser;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户视图对象（Controller 返回给前端的响应对象，不含密码等敏感字段）
 */
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（序列化为字符串，避免前端 JS 精度丢失） */
    private String id;
    private String username;
    private String nickname;
    private String email;
    private String phone;

    /** 身份证号（明文；库中为 SM4 密文，由 Controller 出口处解密回填，from() 不带密文出域） */
    private String idCard;

    private String role;

    /** 所属组织 ID（为空表示未归属） */
    private String orgId;

    /** 所属组织名称（列表展示用，非数据库字段，由接口批量回填） */
    private String orgName;

    private Integer status;
    private String remark;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 已授权应用数（列表展示用，非数据库字段） */
    private Integer authClientCount;

    /** 已绑定设备数（列表展示用，非数据库字段） */
    private Integer deviceCount;

    /** 连续登录失败次数 */
    private Integer loginFailCount;

    /** 锁定截止时间（为空表示未锁定） */
    private LocalDateTime lockedUntil;

    /** 锁定原因 */
    private String lockReason;

    /** 是否需要修改密码：1=下次登录需强制修改（规则配置 - 首次登录强制修改密码） */
    private Integer mustChangePassword;

    /** 由实体转换为视图对象 */
    public static UserVO from(UcUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        // idCard 不在此处赋值：库中为 SM4 密文，由 Controller 出口处解密回填明文（防密文外泄）
        vo.setRole(user.getRole());
        vo.setOrgId(user.getOrgId());
        vo.setStatus(user.getStatus());
        vo.setRemark(user.getRemark());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        vo.setLoginFailCount(user.getLoginFailCount());
        vo.setLockedUntil(user.getLockedUntil());
        vo.setLockReason(user.getLockReason());
        vo.setMustChangePassword(user.getMustChangePassword());
        return vo;
    }

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

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getAuthClientCount() {
        return authClientCount;
    }

    public void setAuthClientCount(Integer authClientCount) {
        this.authClientCount = authClientCount;
    }

    public Integer getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(Integer deviceCount) {
        this.deviceCount = deviceCount;
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
        return "UserVO{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", idCard=[已脱敏]" +
                ", role='" + role + '\'' +
                ", orgId='" + orgId + '\'' +
                ", orgName='" + orgName + '\'' +
                ", status=" + status +
                ", remark='" + remark + '\'' +
                ", lastLoginTime=" + lastLoginTime +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", authClientCount=" + authClientCount +
                ", deviceCount=" + deviceCount +
                ", loginFailCount=" + loginFailCount +
                ", lockedUntil=" + lockedUntil +
                ", lockReason='" + lockReason + '\'' +
                ", mustChangePassword=" + mustChangePassword +
                '}';
    }
}
