package com.bdsoft.authcenter.authz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.authcenter.authz.po.AcRoleMenu;

/**
 * 角色-菜单绑定 Service（通用 CRUD 复用 MyBatis-Plus，绑定关系由 AcRoleService 编排）
 */
public interface AcRoleMenuService extends IService<AcRoleMenu> {

    /** 删除角色时级联清理绑定（显式填删除人/时间后逻辑删除） */
    void removeByRoleId(String roleId);

    /** 删除菜单时级联清理绑定（显式填删除人/时间后逻辑删除） */
    void removeByMenuId(String menuId);
}
