package com.bdsoft.authcenter.controller.authz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdsoft.authcenter.po.authz.AcMenu;
import com.bdsoft.authcenter.po.authz.AcRole;
import com.bdsoft.authcenter.po.authz.AcUserRole;
import com.bdsoft.authcenter.service.authz.AcMenuService;
import com.bdsoft.authcenter.service.authz.AcRoleService;
import com.bdsoft.authcenter.service.authz.AcUserRoleService;
import com.bdsoft.authcenter.support.authz.AppRegistryCache;
import com.bdsoft.authcenter.common.annotation.OperLog;
import com.bdsoft.authcenter.support.usercenter.UserCenterClient;
import com.bdsoft.pkusetoken.util.PkuseResult;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 孤儿数据清理 Controller（V2：应用/用户在用户中心删除后，本地残留授权数据的扫描与清理）
 *
 * <p> 孤儿类型： </p>
 * <ul>
 *   <li><b>孤儿应用数据</b>：菜单/角色引用的 client_id 已不在应用注册表（应用被删除）；</li>
 *   <li><b>孤儿用户绑定</b>：用户-角色绑定的 user_id 在用户中心已不存在（用户被删除）。
 *       用户存在性经用户中心接口探测（不可用时该项标记 unknown，不参与清理）。</li>
 * </ul>
 * <p> 清理操作走操作日志留痕；应用注册表中的「禁用」应用不算孤儿（仅删除的算）。 </p>
 */
@RestController
@RequestMapping("/api/authz/cleanup")
public class CleanupController {

    private static final Logger log = LoggerFactory.getLogger(CleanupController.class);

    @Resource
    private AppRegistryCache registryCache;

    @Resource
    private AcMenuService menuService;

    @Resource
    private AcRoleService roleService;

    @Resource
    private AcUserRoleService userRoleService;

    @Resource
    private UserCenterClient userCenterClient;

    /** 扫描孤儿数据（只读）：孤儿应用（含菜单/角色数）+ 孤儿用户绑定 */
    @GetMapping("/scan")
    public PkuseResult scan() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("registrySize", registryCache.list().size());

        // 1. 孤儿应用：本地引用的 client_id 不在注册表
        Set<String> localClientIds = new TreeSet<>();
        localClientIds.addAll(menuService.list().stream().map(AcMenu::getClientId).collect(Collectors.toSet()));
        localClientIds.addAll(roleService.list().stream().map(AcRole::getClientId).collect(Collectors.toSet()));
        Set<String> registryIds = registryCache.list().stream()
                .map(app -> app.getClientId()).collect(Collectors.toSet());
        List<String> orphanClients = localClientIds.stream()
                .filter(clientId -> !registryIds.contains(clientId))
                .collect(Collectors.toList());
        result.put("orphanClients", orphanClients);

        // 2. 孤儿用户绑定：user_id 逐个经用户中心探测（探测失败的标记 unknown，不误删）
        Set<String> userIds = userRoleService.list().stream()
                .map(AcUserRole::getUserId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> orphanUsers = new ArrayList<>();
        List<String> unknownUsers = new ArrayList<>();
        for (String userId : userIds) {
            try {
                // 用户中心接口：用户不存在时抛业务异常
                userCenterClient.getUserClients(userId);
            } catch (com.bdsoft.authcenter.common.exception.BizException e) {
                if (e.getMessage() != null && e.getMessage().contains("用户不存在")) {
                    orphanUsers.add(userId);
                } else {
                    unknownUsers.add(userId);
                }
            } catch (Exception e) {
                unknownUsers.add(userId);
            }
        }
        result.put("orphanUsers", orphanUsers);
        result.put("unknownUsers", unknownUsers);
        return PkuseResult.ok().setData(result);
    }

    /** 执行清理：删除孤儿应用的菜单/角色（级联绑定）与孤儿用户的绑定 */
    @PostMapping("/execute")
    @OperLog(module = "授权中心-数据清理", action = "清理孤儿数据")
    public PkuseResult execute() {
        // 1. 孤儿应用 → 角色走 deleteRole（级联清理两类绑定），菜单批量逻辑删除
        Set<String> registryIds = registryCache.list().stream()
                .map(app -> app.getClientId()).collect(Collectors.toSet());
        List<AcRole> orphanRoles = roleService.list().stream()
                .filter(role -> !registryIds.contains(role.getClientId()))
                .collect(Collectors.toList());
        for (AcRole role : orphanRoles) {
            roleService.deleteRole(role.getId());
        }
        List<AcMenu> orphanMenus = menuService.list().stream()
                .filter(menu -> !registryIds.contains(menu.getClientId()))
                .collect(Collectors.toList());
        if (!orphanMenus.isEmpty()) {
            List<String> menuIds = orphanMenus.stream().map(AcMenu::getId).collect(Collectors.toList());
            menuService.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AcMenu>()
                    .in(AcMenu::getId, menuIds)
                    .set(AcMenu::getDeleteBy, null)
                    .set(AcMenu::getDeleteTime, LocalDateTime.now()));
            menuService.removeByIds(menuIds);
        }

        // 2. 孤儿用户 → 删除其全部绑定
        Set<String> userIds = userRoleService.list().stream()
                .map(AcUserRole::getUserId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> orphanUsers = new ArrayList<>();
        for (String userId : userIds) {
            try {
                userCenterClient.getUserClients(userId);
            } catch (com.bdsoft.authcenter.common.exception.BizException e) {
                if (e.getMessage() != null && e.getMessage().contains("用户不存在")) {
                    orphanUsers.add(userId);
                }
            } catch (Exception ignored) {
                // 探测失败不清理，避免误删
            }
        }
        userRoleService.removeByUserIds(orphanUsers);

        Map<String, Object> summary = new HashMap<>();
        summary.put("removedRoles", orphanRoles.size());
        summary.put("removedMenus", orphanMenus.size());
        summary.put("removedUserBindings", orphanUsers.size());
        log.info("孤儿数据清理完成：{}", summary);
        return PkuseResult.ok("清理完成").setData(summary);
    }
}
