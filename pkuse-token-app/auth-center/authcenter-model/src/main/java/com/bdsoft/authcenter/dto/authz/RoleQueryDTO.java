package com.bdsoft.authcenter.dto.authz;

import com.bdsoft.authcenter.common.pojo.PageQueryDTO;

/**
 * 角色分页查询参数（DTO）
 */
public class RoleQueryDTO extends PageQueryDTO {

    /** 归属应用标识（必填：角色按应用隔离） */
    private String clientId;

    /** 角色名称/编码（模糊匹配，可空） */
    private String roleName;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
