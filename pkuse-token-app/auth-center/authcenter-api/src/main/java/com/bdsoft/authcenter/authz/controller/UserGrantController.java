package com.bdsoft.authcenter.authz.controller;

import com.bdsoft.authcenter.authz.dto.UserQueryDTO;
import com.bdsoft.authcenter.authz.po.AcRole;
import com.bdsoft.authcenter.authz.po.AcUserRole;
import com.bdsoft.authcenter.authz.service.AcRoleService;
import com.bdsoft.authcenter.authz.service.AcUserRoleService;
import com.bdsoft.authcenter.authz.support.AppRegistryCache;
import com.bdsoft.authcenter.authz.vo.AuthzUserVO;
import com.bdsoft.authcenter.authz.vo.RoleVO;
import com.bdsoft.authcenter.authz.vo.UserRolesVO;
import com.bdsoft.authcenter.common.annotation.OperLog;
import com.bdsoft.authcenter.common.pojo.PageData;
import com.bdsoft.authcenter.usercenter.bo.RegistryUser;
import com.bdsoft.authcenter.usercenter.support.UserCenterClient;
import com.bdsoft.pkusetoken.util.PkuseResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户授权 Controller（用户档案不落地：实时经接口取自用户中心，本地仅角色绑定）
 */
@RestController
@RequestMapping("/api/authz/user")
public class UserGrantController {

    @Resource
    private UserCenterClient userCenterClient;
    @Resource
    private AcRoleService roleService;
    @Resource
    private AcUserRoleService userRoleService;
    @Resource
    private AppRegistryCache registryCache;


    /**
     * 用户分页：用户中心实时数据 + 本地角色摘要回填（用户中心不可用时抛业务异常，前端展示降级提示）
     */
    @GetMapping("/page")
    public PkuseResult page(UserQueryDTO query) {
        RegistryUser.Page page = userCenterClient.getUserPage(query);
        List<String> userIds = page.getRecords().stream()
                .map(RegistryUser::getId).collect(Collectors.toList());
        Map<String, List<AcRole>> rolesByUser = rolesByUserIds(userIds);

        List<AuthzUserVO> records = page.getRecords().stream().map(user -> {
            AuthzUserVO vo = AuthzUserVO.from(user);
            List<AcRole> roles = rolesByUser.getOrDefault(user.getId(), List.of());
            vo.setRoleCount(roles.size());
            vo.setRoleSummary(roles.stream()
                    .map(role -> clientLabel(role.getClientId()) + "：" + role.getRoleName())
                    .collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());

        return PkuseResult.ok().setData(PageData.of(records, page.getTotal(), page.getCurrent(), page.getSize()));
    }

    /**
     * 用户角色分组视图（分配弹窗）：按启用应用分组展示可选角色、已选角色与入口授权提示
     */
    @GetMapping("/{id}/roles")
    public PkuseResult roles(@PathVariable String id) {
        UserRolesVO vo = new UserRolesVO();
        vo.setUserId(id);

        // 入口授权（用户中心不可用时置空，前端不展示提示）
        Set<String> entryClients;
        try {
            entryClients = new HashSet<>(userCenterClient.getUserClients(id));
        } catch (Exception e) {
            entryClients = null;
        }

        // 用户当前绑定
        Set<String> boundRoleIds = userRoleService.listByUserId(id).stream()
                .map(AcUserRole::getRoleId).collect(Collectors.toSet());

        // 按启用应用分组：可选角色（启用）一次性查询后按应用分组
        Map<String, List<RoleVO>> rolesByClient = roleService.list().stream()
                .filter(role -> Integer.valueOf(AcRole.STATUS_ENABLED).equals(role.getStatus()))
                .map(RoleVO::from)
                .collect(Collectors.groupingBy(RoleVO::getClientId));
        for (var app : registryCache.list()) {
            if (!Integer.valueOf(com.bdsoft.authcenter.authz.bo.RegistryApp.STATUS_ENABLED).equals(app.getStatus())) {
                continue;
            }
            UserRolesVO.Group group = new UserRolesVO.Group();
            group.setClientId(app.getClientId());
            group.setClientName(app.getClientName());
            group.setEntryGranted(entryClients == null ? null : entryClients.contains(app.getClientId()));
            List<RoleVO> groupRoles = rolesByClient.getOrDefault(app.getClientId(), List.of());
            Set<String> groupRoleIds = groupRoles.stream().map(RoleVO::getId).collect(Collectors.toSet());
            group.setRoles(groupRoles);
            group.setBoundRoleIds(boundRoleIds.stream()
                    .filter(groupRoleIds::contains)
                    .collect(Collectors.toList()));
            vo.getGroups().add(group);
        }
        return PkuseResult.ok().setData(vo);
    }

    /** 覆盖式保存用户角色（body: roleIds 数组，角色自身携带应用归属） */
    @PutMapping("/{id}/roles")
    @OperLog(module = "授权中心-用户授权", action = "保存用户角色")
    public PkuseResult bindRoles(@PathVariable String id, @RequestBody List<String> roleIds) {
        userRoleService.bindRoles(id, roleIds);
        return PkuseResult.ok("保存成功");
    }

    /** 批量查询用户绑定并按用户分组（角色名称由调用方拼装） */
    private Map<String, List<AcRole>> rolesByUserIds(List<String> userIds) {
        Map<String, List<AcRole>> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        List<AcUserRole> bindings = userRoleService.listByUserIds(userIds);
        if (bindings.isEmpty()) {
            return result;
        }
        Set<String> roleIds = bindings.stream().map(AcUserRole::getRoleId).collect(Collectors.toSet());
        Map<String, AcRole> roleById = roleService.listByIds(roleIds).stream()
                .collect(Collectors.toMap(AcRole::getId, role -> role, (a, b) -> a));
        for (AcUserRole binding : bindings) {
            AcRole role = roleById.get(binding.getRoleId());
            if (role != null) {
                result.computeIfAbsent(binding.getUserId(), k -> new ArrayList<>()).add(role);
            }
        }
        return result;
    }

    /** 应用显示名（注册表缓存） */
    private String clientLabel(String clientId) {
        var app = registryCache.get(clientId);
        return app == null ? clientId : app.getClientName();
    }
}
