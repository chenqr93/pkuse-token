package com.bdsoft.authcenter.authz.controller;

import com.bdsoft.authcenter.authz.po.AcMenu;
import com.bdsoft.authcenter.authz.po.AcRole;
import com.bdsoft.authcenter.authz.po.AcUserRole;
import com.bdsoft.authcenter.authz.service.AcMenuService;
import com.bdsoft.authcenter.authz.service.AcRoleService;
import com.bdsoft.authcenter.authz.service.AcUserRoleService;
import com.bdsoft.authcenter.authz.support.AppRegistryCache;
import com.bdsoft.authcenter.authz.vo.AppVO;
import com.bdsoft.authcenter.common.annotation.OperLog;
import com.bdsoft.pkusetoken.util.PkuseResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用档案管理 Controller（应用不落地：注册表实时查询 + 缓存兜底 + 本地授权统计回填）
 */
@RestController
@RequestMapping("/api/authz/app")
public class AppController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private AppRegistryCache registryCache;
    @Resource
    private AcMenuService menuService;
    @Resource
    private AcRoleService roleService;
    @Resource
    private AcUserRoleService userRoleService;
    @Resource
    private com.bdsoft.authcenter.common.config.AuthCenterProperties properties;


    /**
     * 应用列表：实时刷新注册表（失败回落缓存并标注），回填本地菜单数/角色数/授权用户数
     */
    @GetMapping("/list")
    public PkuseResult list() {
        String source = "live";
        try {
            registryCache.refresh();
        } catch (Exception e) {
            source = "cache";
        }
        String ssoModeName = properties.getSso().isTicketMode()
                ? com.bdsoft.authcenter.common.config.AuthCenterProperties.Sso.MODE_TICKET
                : com.bdsoft.authcenter.common.config.AuthCenterProperties.Sso.MODE_SIMPLE;
        final String dataSource = source;

        // 本地统计批量回填（3 次查询内存分组，避免逐应用查询）
        Map<String, Long> menuCounts = menuService.list().stream()
                .collect(Collectors.groupingBy(AcMenu::getClientId, Collectors.counting()));
        Map<String, Long> roleCounts = roleService.list().stream()
                .collect(Collectors.groupingBy(AcRole::getClientId, Collectors.counting()));
        Map<String, Set<String>> clientUsers = new HashMap<>();
        List<AcRole> allRoles = roleService.list();
        Map<String, String> roleClient = allRoles.stream()
                .collect(Collectors.toMap(AcRole::getId, AcRole::getClientId, (a, b) -> a));
        for (AcUserRole binding : userRoleService.list()) {
            String clientId = roleClient.get(binding.getRoleId());
            if (clientId != null) {
                clientUsers.computeIfAbsent(clientId, k -> new HashSet<>()).add(binding.getUserId());
            }
        }

        List<AppVO> apps = registryCache.list().stream()
                .map(app -> {
                    AppVO vo = AppVO.from(app);
                    vo.setMenuCount(menuCounts.getOrDefault(app.getClientId(), 0L));
                    vo.setRoleCount(roleCounts.getOrDefault(app.getClientId(), 0L));
                    vo.setUserCount((long) clientUsers.getOrDefault(app.getClientId(), new HashSet<>()).size());
                    vo.setLastRefreshTime(registryCache.lastRefreshTime());
                    vo.setSource(dataSource);
                    return vo;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // 统一信封：附加信息收进 data（data={apps, source, ssoMode, lastRefreshTime}）
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("apps", apps);
        data.put("source", source);
        data.put("ssoMode", ssoModeName);
        if (registryCache.lastRefreshTime() != null) {
            data.put("lastRefreshTime", TIME_FORMATTER.format(registryCache.lastRefreshTime()));
        }
        return PkuseResult.ok().setData(data);
    }

    /** 手动刷新注册表缓存（密钥轮换后立即收敛） */
    @PostMapping("/refresh")
    @OperLog(module = "授权中心-应用刷新", action = "手动刷新注册表")
    public PkuseResult refresh() {
        registryCache.refresh();
        return PkuseResult.ok("注册表已刷新")
                .setData(registryCache.list().size());
    }
}
