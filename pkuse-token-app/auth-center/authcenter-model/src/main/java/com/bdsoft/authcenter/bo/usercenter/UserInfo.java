package com.bdsoft.authcenter.bo.usercenter;

import java.io.Serializable;

/**
 * 当前登录用户信息（BO，登录时经用户中心取得，缓存于本地会话；纯身份信息）
 *
 * <p> simple 模式来源：透传 GET /api/auth/me；ticket 模式来源：SSO userinfo 消息。 </p>
 * <p> 用户信息不含角色（认证/授权职责拆分）：控制台自身的管理端角色走本地 RBAC </p>
 * <p>（client=本应用，见 support.security.StpInterfaceImpl），与用户信息通道无关。 </p>
 */
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID（用户中心主键，即本系统 loginId） */
    private String id;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

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

    @Override
    public String toString() {
        return "UserInfo{id='" + id + "', username='" + username + "', nickname='" + nickname + "'}";
    }
}
