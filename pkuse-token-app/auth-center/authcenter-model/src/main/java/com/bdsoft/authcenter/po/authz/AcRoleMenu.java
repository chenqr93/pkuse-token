package com.bdsoft.authcenter.po.authz;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdsoft.authcenter.common.pojo.AbstractAuditPO;

/**
 * 授权中心-角色-菜单绑定表（ac_role_menu）
 */
@TableName("ac_role_menu")
public class AcRoleMenu extends AbstractAuditPO {

    private static final long serialVersionUID = 1L;

    /** 主键（32 位 UUID 字符串） */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 角色 ID（ac_role.id） */
    private String roleId;

    /** 菜单 ID（ac_menu.id） */
    private String menuId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }

    @Override
    public String toString() {
        return "AcRoleMenu{" + super.toString() +
                ", id='" + id + '\'' +
                ", roleId='" + roleId + '\'' +
                ", menuId='" + menuId + '\'' +
                '}';
    }
}
