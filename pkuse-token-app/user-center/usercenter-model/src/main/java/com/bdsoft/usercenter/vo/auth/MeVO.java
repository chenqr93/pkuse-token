package com.bdsoft.usercenter.vo.auth;

import com.bdsoft.usercenter.po.user.UcUser;

import java.io.Serializable;

/**
 * 当前登录用户信息视图对象（纯身份信息，不含角色）
 *
 * <p> 用户信息通道不掺角色（认证/授权职责拆分）： </p>
 * <p> · 平台角色仅用户中心自己控制台使用，走 /api/auth/my-roles； </p>
 * <p> · 应用内角色由应用经授权中心客户端 starter 调 /open/authz/roles 按客户端获取。 </p>
 */
public class MeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（序列化为字符串，避免前端 JS 精度丢失） */
    private String id;
    private String username;
    private String nickname;
    private String email;
    private String phone;

    /** 所属组织 ID（为空表示未归属） */
    private String orgId;

    /** 最后登录时间（展示用） */
    private java.time.LocalDateTime lastLoginTime;

    /** 由实体转换为视图对象 */
    public static MeVO from(UcUser user) {
        if (user == null) {
            return null;
        }
        MeVO vo = new MeVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setOrgId(user.getOrgId());
        vo.setLastLoginTime(user.getLastLoginTime());
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

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public java.time.LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(java.time.LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    @Override
    public String toString() {
        return "MeVO{"
                + "id='" + id + '\''
                + ", username='" + username + '\''
                + ", nickname='" + nickname + '\''
                + ", email='" + email + '\''
                + ", phone='" + phone + '\''
                + ", orgId='" + orgId + '\''
                + '}';
    }
}
