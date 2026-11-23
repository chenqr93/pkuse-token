package com.bdsoft.authcenter.authz.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdsoft.authcenter.common.pojo.AbstractAuditPO;

/**
 * 授权中心-角色表（ac_role，按应用隔离）
 *
 * <p> 应用内角色（非平台角色）：控制用户在各客户端应用内的菜单与按钮权限， </p>
 * <p> 与用户中心 uc_user.role（平台角色，控制两个管理控制台的访问）是两层互不冲突的模型。 </p>
 */
@TableName("ac_role")
public class AcRole extends AbstractAuditPO {

    private static final long serialVersionUID = 1L;

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 0;

    /** 状态：禁用 */
    public static final int STATUS_DISABLED = 1;

    /** 主键（32 位 UUID 字符串） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 归属应用标识（用户中心应用注册表中的 client_id） */
    private String clientId;

    /** 角色编码（应用内唯一，字母数字下划线） */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 状态：0=启用 1=禁用 */
    private Integer status;

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

    @Override
    public String toString() {
        return "AcRole{" + super.toString() +
                ", id='" + id + '\'' +
                ", clientId='" + clientId + '\'' +
                ", roleCode='" + roleCode + '\'' +
                ", roleName='" + roleName + '\'' +
                ", status=" + status +
                '}';
    }
}
