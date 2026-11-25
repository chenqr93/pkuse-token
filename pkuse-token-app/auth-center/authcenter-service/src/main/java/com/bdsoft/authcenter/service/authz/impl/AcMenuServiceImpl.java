package com.bdsoft.authcenter.service.authz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.authcenter.dto.authz.MenuSaveDTO;
import com.bdsoft.authcenter.mapper.authz.AcMenuMapper;
import com.bdsoft.authcenter.po.authz.AcMenu;
import com.bdsoft.authcenter.service.authz.AcMenuService;
import com.bdsoft.authcenter.service.authz.AcRoleMenuService;
import com.bdsoft.authcenter.support.authz.AppRegistryCache;
import com.bdsoft.authcenter.support.authz.AuthzChangePusher;
import com.bdsoft.authcenter.common.exception.BizException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 菜单管理 Service 实现
 */
@Service
public class AcMenuServiceImpl extends ServiceImpl<AcMenuMapper, AcMenu> implements AcMenuService {

    @Resource
    private AppRegistryCache registryCache;

    @Resource
    private AuthzChangePusher changePusher;

    @Resource
    private AcRoleMenuService roleMenuService;

    @Override
    public List<AcMenu> listByClient(String clientId) {
        if (!hasText(clientId)) {
            throw new BizException("应用标识不能为空");
        }
        return this.list(new LambdaQueryWrapper<AcMenu>()
                .eq(AcMenu::getClientId, clientId)
                .orderByAsc(AcMenu::getSort)
                .orderByAsc(AcMenu::getId));
    }

    @Override
    public AcMenu createMenu(MenuSaveDTO param) {
        validateSave(param, null);
        AcMenu menu = new AcMenu();
        copyFields(param, menu);
        menu.setStatus(AcMenu.STATUS_ENABLED);
        menu.setVisible(param.getVisible() == null ? AcMenu.VISIBLE_YES : param.getVisible());
        menu.setSort(param.getSort() == null ? 0 : param.getSort());
        this.save(menu);
        changePusher.push(menu.getClientId(), "菜单管理-新增菜单：" + menu.getMenuName());
        return menu;
    }

    @Override
    public AcMenu updateMenu(MenuSaveDTO param) {
        if (!hasText(param.getId())) {
            throw new BizException("菜单 ID 不能为空");
        }
        AcMenu exists = this.getByIdOrThrow(param.getId());
        validateSave(param, exists);

        AcMenu menu = new AcMenu();
        menu.setId(exists.getId());
        menu.setParentId(hasText(param.getParentId()) ? param.getParentId() : AcMenu.ROOT_PARENT_ID);
        menu.setMenuName(param.getMenuName());
        menu.setMenuType(param.getMenuType());
        menu.setPath(param.getPath());
        menu.setComponent(param.getComponent());
        menu.setPerms(param.getPerms());
        menu.setIcon(param.getIcon());
        menu.setSort(param.getSort() == null ? exists.getSort() : param.getSort());
        menu.setVisible(param.getVisible() == null ? exists.getVisible() : param.getVisible());
        menu.setRemark(param.getRemark());
        this.updateById(menu);
        changePusher.push(menu.getClientId(), "菜单管理-修改菜单：" + menu.getMenuName());
        return menu;
    }

    @Override
    public void deleteMenu(String id) {
        AcMenu menu = this.getByIdOrThrow(id);
        long childCount = this.count(new LambdaQueryWrapper<AcMenu>()
                .eq(AcMenu::getParentId, menu.getId()));
        if (childCount > 0) {
            throw new BizException("该菜单存在子菜单，请先删除子级");
        }
        // 级联清理角色绑定（显式填删除人/时间，随后逻辑删除）
        roleMenuService.removeByMenuId(menu.getId());
        this.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AcMenu>()
                .eq(AcMenu::getId, menu.getId())
                .set(AcMenu::getDeleteBy, currentUserId())
                .set(AcMenu::getDeleteTime, LocalDateTime.now()));
        this.removeById(menu.getId());
        changePusher.push(menu.getClientId(), "菜单管理-删除菜单：" + menu.getMenuName());
    }

    @Override
    public void changeStatus(String id, Integer status) {
        AcMenu menu = this.getByIdOrThrow(id);
        if (status == null || (status != AcMenu.STATUS_ENABLED && status != AcMenu.STATUS_DISABLED)) {
            throw new BizException("状态值不合法（0=启用 1=禁用）");
        }
        AcMenu update = new AcMenu();
        update.setId(menu.getId());
        update.setStatus(status);
        this.updateById(update);
        changePusher.push(menu.getClientId(), "菜单管理-变更菜单状态：" + menu.getMenuName());
    }

    @Override
    public AcMenu getByIdOrThrow(String id) {
        AcMenu menu = hasText(id) ? this.getById(id) : null;
        if (menu == null) {
            throw new BizException("菜单不存在");
        }
        return menu;
    }

    // ------------------------- 校验 -------------------------

    /** 新增/修改统一校验（exclude 为更新场景的原记录，用于排除自身的唯一性校验） */
    private void validateSave(MenuSaveDTO param, AcMenu exclude) {
        if (!hasText(param.getClientId())) {
            throw new BizException("应用标识不能为空");
        }
        if (!registryCache.isEnabled(param.getClientId())) {
            throw new BizException("应用不存在或未启用（可在应用列表刷新注册表后重试）");
        }
        if (exclude != null && !Objects.equals(exclude.getClientId(), param.getClientId())) {
            throw new BizException("归属应用不允许修改");
        }
        if (!hasText(param.getMenuName())) {
            throw new BizException("菜单名称不能为空");
        }
        Integer type = param.getMenuType();
        if (type == null || type < AcMenu.TYPE_DIRECTORY || type > AcMenu.TYPE_BUTTON) {
            throw new BizException("菜单类型不合法（0=目录 1=页面 2=按钮）");
        }

        // 父级校验：存在且同应用；更新时不可移动到自身或其子孙下
        String parentId = hasText(param.getParentId()) ? param.getParentId() : AcMenu.ROOT_PARENT_ID;
        if (!AcMenu.ROOT_PARENT_ID.equals(parentId)) {
            AcMenu parent = this.getById(parentId);
            if (parent == null) {
                throw new BizException("父菜单不存在");
            }
            if (!Objects.equals(parent.getClientId(), param.getClientId())) {
                throw new BizException("父菜单不属于该应用");
            }
            if (exclude != null && isSelfOrDescendant(parent, exclude.getId())) {
                throw new BizException("上级菜单不能选择自身或其子孙节点");
            }
        }

        // 类型字段约束
        String perms = trimToNull(param.getPerms());
        if (type == AcMenu.TYPE_BUTTON) {
            if (perms == null) {
                throw new BizException("按钮必须配置权限标识（perms）");
            }
            if (hasText(param.getPath()) || hasText(param.getComponent())) {
                throw new BizException("按钮不需要路由地址与组件路径");
            }
        } else if (type == AcMenu.TYPE_DIRECTORY) {
            if (perms != null) {
                throw new BizException("目录不允许配置权限标识（请配置在页面或按钮上）");
            }
            if (hasText(param.getComponent())) {
                throw new BizException("目录不需要组件路径");
            }
        } else {
            if (!hasText(param.getPath())) {
                throw new BizException("页面必须配置路由地址（path）");
            }
        }

        // 应用内唯一：菜单名（更新场景排除自身；条件值必须显式分支，避免条件参数被急切求值）
        LambdaQueryWrapper<AcMenu> nameWrapper = new LambdaQueryWrapper<AcMenu>()
                .eq(AcMenu::getClientId, param.getClientId())
                .eq(AcMenu::getMenuName, param.getMenuName());
        if (exclude != null) {
            nameWrapper.ne(AcMenu::getId, exclude.getId());
        }
        if (this.count(nameWrapper) > 0) {
            throw new BizException("菜单名称已存在：" + param.getMenuName());
        }
        // 应用内唯一：权限码
        if (perms != null) {
            LambdaQueryWrapper<AcMenu> permsWrapper = new LambdaQueryWrapper<AcMenu>()
                    .eq(AcMenu::getClientId, param.getClientId())
                    .eq(AcMenu::getPerms, perms);
            if (exclude != null) {
                permsWrapper.ne(AcMenu::getId, exclude.getId());
            }
            if (this.count(permsWrapper) > 0) {
                throw new BizException("权限标识已被使用：" + perms);
            }
        }
        if (perms != null && !perms.matches("[a-zA-Z0-9:_\\-]+")) {
            throw new BizException("权限标识仅允许字母、数字、冒号、下划线与中划线：" + perms);
        }
    }

    /** 判断 parent 是否为 menuId 自身或其子孙节点（沿 parent.parentId 向上回溯） */
    private boolean isSelfOrDescendant(AcMenu parent, String menuId) {
        String cursor = parent.getId();
        while (cursor != null && !AcMenu.ROOT_PARENT_ID.equals(cursor)) {
            if (cursor.equals(menuId)) {
                return true;
            }
            AcMenu current = this.getById(cursor);
            cursor = current == null ? null : current.getParentId();
        }
        return false;
    }

    private void copyFields(MenuSaveDTO param, AcMenu menu) {
        menu.setClientId(param.getClientId());
        menu.setParentId(hasText(param.getParentId()) ? param.getParentId() : AcMenu.ROOT_PARENT_ID);
        menu.setMenuName(param.getMenuName());
        menu.setMenuType(param.getMenuType());
        menu.setPath(param.getPath());
        menu.setComponent(param.getComponent());
        menu.setPerms(trimToNull(param.getPerms()));
        menu.setIcon(param.getIcon());
        menu.setRemark(param.getRemark());
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

    private static String trimToNull(String text) {
        return hasText(text) ? text.trim() : null;
    }
}
