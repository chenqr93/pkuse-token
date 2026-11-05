package com.bdsoft.usercenter.open.vo;

import com.bdsoft.usercenter.user.po.UcUser;

import java.io.Serializable;

/**
 * 用户注册表条目（服务间开放接口 /open/registry/users 返回）
 *
 * <p> 供授权中心控制台查询用户（不落库，实时透传），不含密码等敏感字段。 </p>
 */
public class RegistryUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String id;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 平台角色：admin=系统管理员 / user=普通用户 */
    private String role;

    /** 状态：0=禁用 / 1=启用 */
    private Integer status;

    public static RegistryUserVO from(UcUser user) {
        if (user == null) {
            return null;
        }
        RegistryUserVO vo = new RegistryUserVO();
        vo.id = user.getId();
        vo.username = user.getUsername();
        vo.nickname = user.getNickname();
        vo.role = user.getRole();
        vo.status = user.getStatus();
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
}
