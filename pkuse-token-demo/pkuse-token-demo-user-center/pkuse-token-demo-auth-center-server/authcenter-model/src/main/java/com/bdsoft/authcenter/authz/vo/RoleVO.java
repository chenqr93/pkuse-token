package com.bdsoft.authcenter.authz.vo;

import com.bdsoft.authcenter.authz.po.AcRole;

import java.io.Serializable;

/**
 * 角色视图（VO，含列表回填统计）
 */
public class RoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    /** 归属应用标识 */
    private String clientId;

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 状态：0=启用 1=禁用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 已绑菜单数（列表回填） */
    private Integer menuCount;

    /** 已授权用户数（列表回填） */
    private Integer userCount;

    public static RoleVO from(AcRole role) {
        if (role == null) {
            return null;
        }
        RoleVO vo = new RoleVO();
        vo.id = role.getId();
        vo.clientId = role.getClientId();
        vo.roleCode = role.getRoleCode();
        vo.roleName = role.getRoleName();
        vo.status = role.getStatus();
        vo.remark = role.getRemark();
        return vo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
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

    public Integer getMenuCount() {
        return menuCount;
    }

    public void setMenuCount(Integer menuCount) {
        this.menuCount = menuCount;
    }

    public Integer getUserCount() {
        return userCount;
    }

    public void setUserCount(Integer userCount) {
        this.userCount = userCount;
    }
}
