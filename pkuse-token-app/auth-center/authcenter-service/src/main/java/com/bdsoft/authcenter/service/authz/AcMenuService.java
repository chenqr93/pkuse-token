package com.bdsoft.authcenter.service.authz;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.authcenter.dto.authz.MenuSaveDTO;
import com.bdsoft.authcenter.po.authz.AcMenu;

import java.util.List;

/**
 * 菜单管理 Service（按应用隔离）
 *
 * <p> 应用有效性以注册表缓存为准（应用档案不落地）；删除被角色绑定的菜单时级联清理绑定。 </p>
 */
public interface AcMenuService extends IService<AcMenu> {

    /** 查询应用全部菜单（含禁用，按 sort 升序、同级稳定） */
    List<AcMenu> listByClient(String clientId);

    /** 新增菜单（校验应用启用、父级合法、类型字段约束、应用内名称/权限码唯一） */
    AcMenu createMenu(MenuSaveDTO param);

    /** 修改菜单（clientId 不可改；父级不可移动到自身或其子孙下） */
    AcMenu updateMenu(MenuSaveDTO param);

    /** 删除菜单（有子菜单先拦截；级联清理角色绑定并留痕） */
    void deleteMenu(String id);

    /** 启用/禁用菜单 */
    void changeStatus(String id, Integer status);

    /** 根据 ID 查询（不存在时抛业务异常） */
    AcMenu getByIdOrThrow(String id);
}
