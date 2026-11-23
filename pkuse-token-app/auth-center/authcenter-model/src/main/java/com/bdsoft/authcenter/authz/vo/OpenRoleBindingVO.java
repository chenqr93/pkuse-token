package com.bdsoft.authcenter.authz.vo;

import com.bdsoft.authcenter.authz.po.AcRole;
import com.bdsoft.authcenter.authz.po.AcUserRole;

import java.io.Serializable;

/**
 * 用户-角色绑定（开放分发接口 /open/authz/roles/bindings 返回）
 *
 * <p> userId 为用户中心用户主键（即业务应用 SSO loginId），供业务应用同步本地用户-角色副本。 </p>
 */
public class OpenRoleBindingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID（用户中心用户主键 = 业务应用 loginId） */
    private String userId;

    /** 角色 ID */
    private String roleId;

    /** 角色编码（冗余自角色定义，便于调用方直用） */
    private String roleCode;

    /** 角色名称（冗余） */
    private String roleName;

    /** 角色状态：0=启用 / 1=禁用（冗余，调用方可过滤） */
    private Integer roleStatus;

    public static OpenRoleBindingVO from(AcUserRole binding, AcRole role) {
        OpenRoleBindingVO vo = new OpenRoleBindingVO();
        vo.userId = binding.getUserId();
        vo.roleId = binding.getRoleId();
        if (role != null) {
            vo.roleCode = role.getRoleCode();
            vo.roleName = role.getRoleName();
            vo.roleStatus = role.getStatus();
        }
        return vo;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Integer getRoleStatus() {
        return roleStatus;
    }

    public void setRoleStatus(Integer roleStatus) {
        this.roleStatus = roleStatus;
    }
}
