package com.bdsoft.authcenter.authz.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.authcenter.authz.dto.RoleQueryDTO;
import com.bdsoft.authcenter.authz.dto.RoleSaveDTO;
import com.bdsoft.authcenter.authz.po.AcRole;

import java.util.List;
import java.util.Map;

/**
 * 角色管理 Service（按应用隔离）
 */
public interface AcRoleService extends IService<AcRole> {

    /** 分页查询（按应用过滤，支持角色名称/编码模糊） */
    Page<AcRole> pageQuery(RoleQueryDTO query);

    /** 新增角色（校验应用启用、编码格式与应用内唯一） */
    AcRole createRole(RoleSaveDTO param);

    /** 修改角色（clientId 不可改） */
    AcRole updateRole(RoleSaveDTO param);

    /** 删除角色（级联清理角色-菜单与用户-角色绑定并留痕） */
    void deleteRole(String id);

    /** 启用/禁用角色 */
    void changeStatus(String id, Integer status);

    /** 角色已绑菜单 ID 列表 */
    List<String> listMenuIds(String roleId);

    /** 覆盖式保存角色菜单（校验菜单归属同应用；差异增删，保留未变绑定） */
    void bindMenus(String roleId, List<String> menuIds);

    /** 根据 ID 查询（不存在时抛业务异常） */
    AcRole getByIdOrThrow(String id);

    /** 查询应用全量角色（含禁用，按角色编码升序；开放分发接口同步副本用） */
    List<AcRole> listByClient(String clientId);

    /** 批量统计：角色 ID -> 已绑菜单数 */
    Map<String, Integer> countMenusByRoleIds(List<String> roleIds);

    /** 批量统计：角色 ID -> 已授权用户数 */
    Map<String, Integer> countUsersByRoleIds(List<String> roleIds);
}
