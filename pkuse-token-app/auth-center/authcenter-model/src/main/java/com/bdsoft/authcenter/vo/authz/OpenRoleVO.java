package com.bdsoft.authcenter.vo.authz;

import com.bdsoft.authcenter.po.authz.AcRole;

import java.io.Serializable;

/**
 * 应用角色定义（开放分发接口 /open/authz/roles/list 返回）
 *
 * <p> 供业务应用同步本地角色副本（角色码/名称/状态），含禁用角色供调用方自行过滤。 </p>
 */
public class OpenRoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色 ID */
    private String id;

    /** 角色编码（应用内唯一） */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 状态：0=启用 / 1=禁用 */
    private Integer status;

    public static OpenRoleVO from(AcRole role) {
        if (role == null) {
            return null;
        }
        OpenRoleVO vo = new OpenRoleVO();
        vo.id = role.getId();
        vo.roleCode = role.getRoleCode();
        vo.roleName = role.getRoleName();
        vo.status = role.getStatus();
        return vo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}
