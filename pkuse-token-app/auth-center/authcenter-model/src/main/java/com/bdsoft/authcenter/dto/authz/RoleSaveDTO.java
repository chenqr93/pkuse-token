package com.bdsoft.authcenter.dto.authz;

import java.io.Serializable;

/**
 * 角色新增/修改入参（DTO）
 */
public class RoleSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（修改时必填） */
    private String id;

    /** 归属应用标识 */
    private String clientId;

    /** 角色编码（字母数字下划线，应用内唯一） */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 备注 */
    private String remark;

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
