package com.bdsoft.authcenter.client;

import com.bdsoft.pkusetoken.stp.StpUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 菜单/权限码端点（expose-endpoints=true 时由自动装配注册）
 *
 * <p> GET {path-prefix}/menus：当前登录用户的可见菜单树（前端动态菜单渲染） </p>
 * <p> GET {path-prefix}/perms：当前登录用户的权限码集合（前端按钮显隐控制） </p>
 */
@RestController
public class AuthzClientController {

    private final AuthzClientService authzClientService;
    private final AuthCenterClientProperties properties;

    public AuthzClientController(AuthzClientService authzClientService,
                                 AuthCenterClientProperties properties) {
        this.authzClientService = authzClientService;
        this.properties = properties;
    }

    @GetMapping("${auth-center.client.path-prefix:/authz}/menus")
    public Object menus() {
        requireLogin();
        return authzClientService.getMenus(StpUtil.getLoginId().toString());
    }

    @GetMapping("${auth-center.client.path-prefix:/authz}/perms")
    public Object perms() {
        requireLogin();
        return authzClientService.getPerms(StpUtil.getLoginId().toString());
    }

    private static void requireLogin() {
        if (!StpUtil.isLogin()) {
            throw new IllegalStateException("尚未登录，无法获取权限数据");
        }
    }
}
