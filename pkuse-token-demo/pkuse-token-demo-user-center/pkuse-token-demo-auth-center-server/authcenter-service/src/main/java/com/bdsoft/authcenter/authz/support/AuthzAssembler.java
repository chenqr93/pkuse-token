package com.bdsoft.authcenter.authz.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdsoft.authcenter.authz.po.AcMenu;
import com.bdsoft.authcenter.authz.po.AcRole;
import com.bdsoft.authcenter.authz.po.AcRoleMenu;
import com.bdsoft.authcenter.authz.po.AcUserRole;
import com.bdsoft.authcenter.authz.service.AcMenuService;
import com.bdsoft.authcenter.authz.service.AcRoleService;
import com.bdsoft.authcenter.authz.service.AcRoleMenuService;
import com.bdsoft.authcenter.authz.service.AcUserRoleService;
import com.bdsoft.authcenter.authz.vo.MenuTreeVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 授权数据装配器：菜单树 / 权限码集合（管理端与开放分发接口共用）
 *
 * <p> 分发口径：多角色并集；仅启用角色、启用菜单参与；菜单树取「启用 + 显示」的目录/页面， </p>
 * <p> 并向上补齐祖先节点保证树连通；权限码取所有启用菜单上非空的 perms 并集（含隐藏菜单）。 </p>
 */
@Component
public class AuthzAssembler {

    private final AcMenuService menuService;
    private final AcRoleService roleService;
    private final AcRoleMenuService roleMenuService;
    private final AcUserRoleService userRoleService;

    public AuthzAssembler(AcMenuService menuService,
                          AcRoleService roleService,
                          AcRoleMenuService roleMenuService,
                          AcUserRoleService userRoleService) {
        this.menuService = menuService;
        this.roleService = roleService;
        this.roleMenuService = roleMenuService;
        this.userRoleService = userRoleService;
    }

    /** 管理端菜单树：应用全部菜单（含禁用、含按钮），按 sort 升序 */
    public List<MenuTreeVO> consoleTree(String clientId) {
        List<AcMenu> all = menuService.listByClient(clientId);
        return buildTree(all, null);
    }

    /**
     * 分发菜单树：用户在某应用「启用 + 显示」的目录/页面（多角色并集，向上补齐祖先）
     */
    public List<MenuTreeVO> distributeMenus(String clientId, String userId) {
        Set<String> menuIds = boundMenuIds(clientId, userId);
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<AcMenu> all = menuService.listByClient(clientId);
        Map<String, AcMenu> byId = all.stream()
                .collect(Collectors.toMap(AcMenu::getId, m -> m, (a, b) -> a));

        // 选中的启用+显示 目录/页面，并向上补齐祖先（保证树连通）
        Set<String> includeIds = new LinkedHashSet<>();
        for (String menuId : menuIds) {
            AcMenu menu = byId.get(menuId);
            if (menu == null || !Integer.valueOf(AcMenu.STATUS_ENABLED).equals(menu.getStatus())
                    || !Integer.valueOf(AcMenu.VISIBLE_YES).equals(menu.getVisible())
                    || menu.getMenuType() == null || menu.getMenuType() > AcMenu.TYPE_PAGE) {
                continue;
            }
            String cursor = menuId;
            while (cursor != null && includeIds.add(cursor)) {
                AcMenu current = byId.get(cursor);
                cursor = current == null ? null : current.getParentId();
            }
        }
        return buildTree(all, includeIds);
    }

    /**
     * 分发权限码集合：用户在某应用所有启用菜单上非空 perms 的并集（含隐藏菜单与按钮）
     */
    public List<String> distributePerms(String clientId, String userId) {
        Set<String> menuIds = boundMenuIds(clientId, userId);
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        return menuService.list(new LambdaQueryWrapper<AcMenu>()
                        .eq(AcMenu::getClientId, clientId)
                        .eq(AcMenu::getStatus, AcMenu.STATUS_ENABLED)
                        .in(AcMenu::getId, menuIds)
                        .isNotNull(AcMenu::getPerms))
                .stream()
                .map(AcMenu::getPerms)
                .filter(perms -> perms != null && !perms.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 分发角色编码集合：用户在某应用绑定的「启用」角色编码列表（去重排序；无绑定返回空列表）
     */
    public List<String> distributeRoles(String clientId, String userId) {
        List<AcUserRole> userRoles = userRoleService.listByUserId(userId);
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> roleIds = userRoles.stream().map(AcUserRole::getRoleId).collect(Collectors.toList());
        return roleService.listByIds(roleIds).stream()
                .filter(role -> Integer.valueOf(AcRole.STATUS_ENABLED).equals(role.getStatus()))
                .filter(role -> Objects.equals(role.getClientId(), clientId))
                .map(AcRole::getRoleCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /** 用户在某应用绑定的菜单 ID 集合（经启用角色；用户无角色返回空集） */
    private Set<String> boundMenuIds(String clientId, String userId) {
        List<AcUserRole> userRoles = userRoleService.listByUserId(userId);
        if (userRoles.isEmpty()) {
            return Collections.emptySet();
        }
        List<String> roleIds = userRoles.stream().map(AcUserRole::getRoleId).collect(Collectors.toList());
        // 仅启用角色生效
        List<AcRole> roles = roleService.listByIds(roleIds).stream()
                .filter(role -> Integer.valueOf(AcRole.STATUS_ENABLED).equals(role.getStatus()))
                .collect(Collectors.toList());
        if (roles.isEmpty()) {
            return Collections.emptySet();
        }
        List<String> enabledRoleIds = roles.stream().map(AcRole::getId).collect(Collectors.toList());
        return roleMenuService.list(new LambdaQueryWrapper<AcRoleMenu>()
                        .in(AcRoleMenu::getRoleId, enabledRoleIds))
                .stream()
                .map(AcRoleMenu::getMenuId)
                .collect(Collectors.toSet());
    }

    /** 构树：includeIds 为 null 表示全部节点；按 sort 升序、同级稳定 */
    private List<MenuTreeVO> buildTree(List<AcMenu> all, Set<String> includeIds) {
        Map<String, List<AcMenu>> byParent = new HashMap<>();
        for (AcMenu menu : all) {
            if (includeIds == null || includeIds.contains(menu.getId())) {
                byParent.computeIfAbsent(menu.getParentId(), k -> new ArrayList<>()).add(menu);
            }
        }
        Comparator<AcMenu> bySort = Comparator
                .comparing((AcMenu m) -> m.getSort() == null ? 0 : m.getSort())
                .thenComparing(AcMenu::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        return buildChildren(AcMenu.ROOT_PARENT_ID, byParent, bySort);
    }

    private List<MenuTreeVO> buildChildren(String parentId,
                                           Map<String, List<AcMenu>> byParent,
                                           Comparator<AcMenu> bySort) {
        List<AcMenu> children = byParent.get(parentId);
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        children.sort(bySort);
        List<MenuTreeVO> result = new ArrayList<>();
        for (AcMenu menu : children) {
            MenuTreeVO vo = MenuTreeVO.from(menu);
            vo.setChildren(buildChildren(menu.getId(), byParent, bySort));
            result.add(vo);
        }
        return result;
    }
}
