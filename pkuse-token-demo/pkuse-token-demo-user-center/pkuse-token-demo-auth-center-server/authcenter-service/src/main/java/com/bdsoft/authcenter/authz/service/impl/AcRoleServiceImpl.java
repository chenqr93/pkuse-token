package com.bdsoft.authcenter.authz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.authcenter.authz.dto.RoleQueryDTO;
import com.bdsoft.authcenter.authz.dto.RoleSaveDTO;
import com.bdsoft.authcenter.authz.mapper.AcRoleMapper;
import com.bdsoft.authcenter.authz.po.AcMenu;
import com.bdsoft.authcenter.authz.po.AcRole;
import com.bdsoft.authcenter.authz.po.AcRoleMenu;
import com.bdsoft.authcenter.authz.service.AcMenuService;
import com.bdsoft.authcenter.authz.service.AcRoleMenuService;
import com.bdsoft.authcenter.authz.service.AcRoleService;
import com.bdsoft.authcenter.authz.service.AcUserRoleService;
import com.bdsoft.authcenter.authz.support.AppRegistryCache;
import com.bdsoft.authcenter.authz.support.AuthzChangePusher;
import com.bdsoft.authcenter.common.exception.BizException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色管理 Service 实现
 */
@Service
public class AcRoleServiceImpl extends ServiceImpl<AcRoleMapper, AcRole> implements AcRoleService {

    @Resource
    private AppRegistryCache registryCache;

    @Resource
    private AuthzChangePusher changePusher;

    @Resource
    private AcMenuService menuService;

    @Resource
    private AcRoleMenuService roleMenuService;

    @Resource
    private AcUserRoleService userRoleService;

    @Override
    public Page<AcRole> pageQuery(RoleQueryDTO query) {
        if (!hasText(query.getClientId())) {
            throw new BizException("应用标识不能为空（角色按应用隔离）");
        }
        return this.page(new Page<>(query.currentOrDefault(), query.sizeOrDefault()),
                new LambdaQueryWrapper<AcRole>()
                        .eq(AcRole::getClientId, query.getClientId())
                        .like(hasText(query.getRoleName()), AcRole::getRoleName, query.getRoleName())
                        .or(hasText(query.getRoleName()),
                                wrapper -> wrapper.eq(AcRole::getClientId, query.getClientId())
                                        .like(AcRole::getRoleCode, query.getRoleName()))
                        .orderByAsc(AcRole::getRoleCode));
    }

    @Override
    public AcRole createRole(RoleSaveDTO param) {
        validateSave(param, null);
        AcRole role = new AcRole();
        role.setClientId(param.getClientId());
        role.setRoleCode(param.getRoleCode().trim());
        role.setRoleName(param.getRoleName().trim());
        role.setStatus(AcRole.STATUS_ENABLED);
        role.setRemark(param.getRemark());
        this.save(role);
        changePusher.push(role.getClientId(), "角色管理-新增角色：" + role.getRoleCode());
        return role;
    }

    @Override
    public AcRole updateRole(RoleSaveDTO param) {
        if (!hasText(param.getId())) {
            throw new BizException("角色 ID 不能为空");
        }
        AcRole exists = this.getByIdOrThrow(param.getId());
        validateSave(param, exists);
        AcRole role = new AcRole();
        role.setId(exists.getId());
        role.setRoleName(hasText(param.getRoleName()) ? param.getRoleName().trim() : exists.getRoleName());
        role.setRemark(param.getRemark());
        this.updateById(role);
        changePusher.push(exists.getClientId(), "角色管理-修改角色：" + exists.getRoleCode());
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(String id) {
        AcRole role = this.getByIdOrThrow(id);
        // 级联清理角色-菜单与用户-角色绑定（显式填删除人/时间后逻辑删除）
        roleMenuService.removeByRoleId(role.getId());
        userRoleService.removeByRoleId(role.getId());
        this.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AcRole>()
                .eq(AcRole::getId, role.getId())
                .set(AcRole::getDeleteBy, currentUserId())
                .set(AcRole::getDeleteTime, LocalDateTime.now()));
        this.removeById(role.getId());
        changePusher.push(role.getClientId(), "角色管理-删除角色：" + role.getRoleCode());
    }

    @Override
    public void changeStatus(String id, Integer status) {
        AcRole role = this.getByIdOrThrow(id);
        if (status == null || (status != AcRole.STATUS_ENABLED && status != AcRole.STATUS_DISABLED)) {
            throw new BizException("状态值不合法（0=启用 1=禁用）");
        }
        AcRole update = new AcRole();
        update.setId(role.getId());
        update.setStatus(status);
        this.updateById(update);
        changePusher.push(role.getClientId(), "角色管理-变更角色状态：" + role.getRoleCode());
    }

    @Override
    public List<String> listMenuIds(String roleId) {
        this.getByIdOrThrow(roleId);
        return this.roleMenuMenuIds(List.of(roleId)).get(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindMenus(String roleId, List<String> menuIds) {
        AcRole role = this.getByIdOrThrow(roleId);
        Set<String> target = menuIds == null ? Set.of() : new LinkedHashSet<>(menuIds);
        // 校验菜单存在且归属同应用
        if (!target.isEmpty()) {
            List<AcMenu> menus = menuService.listByIds(target);
            if (menus.size() != target.size()) {
                throw new BizException("存在无效菜单，请刷新后重试");
            }
            for (AcMenu menu : menus) {
                if (!Objects.equals(menu.getClientId(), role.getClientId())) {
                    throw new BizException("菜单不属于该应用：" + menu.getMenuName());
                }
            }
        }
        // 差异增删：保留未变绑定，新增缺失，移除多余（填删除人/时间）
        List<AcRoleMenu> current = roleMenuService.list(new LambdaQueryWrapper<AcRoleMenu>()
                .eq(AcRoleMenu::getRoleId, role.getId()));
        Set<String> currentIds = current.stream().map(AcRoleMenu::getMenuId).collect(Collectors.toSet());

        List<AcRoleMenu> toAdd = target.stream()
                .filter(menuId -> !currentIds.contains(menuId))
                .map(menuId -> {
                    AcRoleMenu binding = new AcRoleMenu();
                    binding.setRoleId(role.getId());
                    binding.setMenuId(menuId);
                    return binding;
                })
                .collect(Collectors.toList());
        List<String> toRemoveIds = current.stream()
                .map(AcRoleMenu::getId)
                .filter(bindingId -> {
                    AcRoleMenu binding = current.stream()
                            .filter(b -> b.getId().equals(bindingId)).findFirst().orElse(null);
                    return binding != null && !target.contains(binding.getMenuId());
                })
                .collect(Collectors.toList());

        if (!toAdd.isEmpty()) {
            roleMenuService.saveBatch(toAdd);
        }
        if (!toRemoveIds.isEmpty()) {
            roleMenuService.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AcRoleMenu>()
                    .in(AcRoleMenu::getId, toRemoveIds)
                    .set(AcRoleMenu::getDeleteBy, currentUserId())
                    .set(AcRoleMenu::getDeleteTime, LocalDateTime.now()));
            roleMenuService.removeByIds(toRemoveIds);
        }
        changePusher.push(role.getClientId(), "角色管理-保存角色菜单：" + role.getRoleCode());
    }

    @Override
    public AcRole getByIdOrThrow(String id) {
        AcRole role = hasText(id) ? this.getById(id) : null;
        if (role == null) {
            throw new BizException("角色不存在");
        }
        return role;
    }

    @Override
    public Map<String, Integer> countMenusByRoleIds(List<String> roleIds) {
        return roleMenuMenuIds(roleIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
    }

    @Override
    public Map<String, Integer> countUsersByRoleIds(List<String> roleIds) {
        return userRoleService.countUsersByRoleIds(roleIds);
    }

    /** 批量查询角色绑定的菜单 ID */
    private Map<String, List<String>> roleMenuMenuIds(List<String> roleIds) {
        Map<String, List<String>> result = new HashMap<>();
        for (String roleId : roleIds) {
            result.put(roleId, new ArrayList<>());
        }
        if (roleIds.isEmpty()) {
            return result;
        }
        roleMenuService.list(new LambdaQueryWrapper<AcRoleMenu>()
                        .in(AcRoleMenu::getRoleId, roleIds))
                .forEach(binding -> result
                        .computeIfAbsent(binding.getRoleId(), k -> new ArrayList<>())
                        .add(binding.getMenuId()));
        return result;
    }

    // ------------------------- 校验 -------------------------

    private void validateSave(RoleSaveDTO param, AcRole exclude) {
        if (exclude == null) {
            if (!hasText(param.getClientId())) {
                throw new BizException("应用标识不能为空");
            }
            if (!registryCache.isEnabled(param.getClientId())) {
                throw new BizException("应用不存在或未启用（可在应用列表刷新注册表后重试）");
            }
            if (!hasText(param.getRoleCode())) {
                throw new BizException("角色编码不能为空");
            }
            if (!param.getRoleCode().trim().matches("[a-zA-Z0-9_]+")) {
                throw new BizException("角色编码仅允许字母、数字与下划线");
            }
            if (!hasText(param.getRoleName())) {
                throw new BizException("角色名称不能为空");
            }
            long count = this.count(new LambdaQueryWrapper<AcRole>()
                    .eq(AcRole::getClientId, param.getClientId())
                    .eq(AcRole::getRoleCode, param.getRoleCode().trim()));
            if (count > 0) {
                throw new BizException("角色编码已存在：" + param.getRoleCode());
            }
        }
    }

    private static String currentUserId() {
        try {
            Object loginId = com.bdsoft.pkusetoken.stp.StpUtil.getLoginIdDefaultNull();
            return loginId == null ? null : loginId.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
