package com.bdsoft.authcenter.vo.authz;

import com.bdsoft.authcenter.bo.usercenter.RegistryUser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户授权-用户列表行（VO：用户中心实时数据 + 本地角色摘要回填）
 */
public class AuthzUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    /** 登录账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 平台角色 */
    private String role;

    /** 状态：0=禁用 1=启用 */
    private Integer status;

    /** 角色摘要（回填）：如「客户端3:普通用户」 */
    private List<String> roleSummary = new ArrayList<>();

    /** 已授权角色数（回填） */
    private Integer roleCount;

    public static AuthzUserVO from(RegistryUser user) {
        if (user == null) {
            return null;
        }
        AuthzUserVO vo = new AuthzUserVO();
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

    public List<String> getRoleSummary() {
        return roleSummary;
    }

    public void setRoleSummary(List<String> roleSummary) {
        this.roleSummary = roleSummary;
    }

    public Integer getRoleCount() {
        return roleCount;
    }

    public void setRoleCount(Integer roleCount) {
        this.roleCount = roleCount;
    }
}
