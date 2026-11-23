package com.bdsoft.authcenter.authz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bdsoft.authcenter.authz.po.AcUserRole;

import java.util.List;
import java.util.Map;

/**
 * 用户-角色绑定 Service（用户档案不落地，user_id 为用户中心用户 ID 外部引用）
 */
public interface AcUserRoleService extends IService<AcUserRole> {

    /** 覆盖式保存用户角色（全量覆盖：角色自身携带应用归属；差异增删，校验角色存在且启用） */
    void bindRoles(String userId, List<String> roleIds);

    /** 查询用户全部绑定 */
    List<AcUserRole> listByUserId(String userId);

    /** 批量查询多个用户的绑定（用户列表角色摘要回填用） */
    List<AcUserRole> listByUserIds(List<String> userIds);

    /** 批量查询多个角色的全部绑定（开放分发接口同步用户-角色副本用） */
    List<AcUserRole> listByRoleIds(List<String> roleIds);

    /** 删除角色时级联清理绑定（显式填删除人/时间后逻辑删除） */
    void removeByRoleId(String roleId);

    /** 孤儿数据清理：删除指定用户的全部绑定（用户已在用户中心删除，显式填删除人/时间后逻辑删除） */
    void removeByUserIds(List<String> userIds);

    /** 已授权用户数（去重 user_id，概览统计） */
    long countDistinctUsers();

    /** 某应用已授权用户数（经该应用角色去重 user_id，应用列表回填） */
    long countDistinctUsersByClientId(String clientId);

    /** 批量统计：角色 ID -> 已授权用户数 */
    Map<String, Integer> countUsersByRoleIds(List<String> roleIds);
}
