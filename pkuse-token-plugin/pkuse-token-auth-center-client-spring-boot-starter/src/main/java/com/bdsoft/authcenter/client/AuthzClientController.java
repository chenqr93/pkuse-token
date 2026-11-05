package com.bdsoft.authcenter.client;

import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 菜单/权限码/角色端点（expose-endpoints=true 时由自动装配注册）
 *
 * <p> 统一返回信封 {"code":200,"msg":"ok","data":...}；未登录返回 code=401。 </p>
 *
 * <p> GET {path-prefix}/menus：当前登录用户的可见菜单树（前端动态菜单渲染） </p>
 * <p> GET {path-prefix}/perms：当前登录用户的权限码集合（前端按钮显隐控制） </p>
 * <p> GET {path-prefix}/roles：当前登录用户的角色编码集合（前端角色标签/路由门禁） </p>
 */
@RestController
public class AuthzClientController {

    private static final String NOT_LOGIN_MSG = "尚未登录，无法获取权限数据";

    private final AuthzClientService authzClientService;

    public AuthzClientController(AuthzClientService authzClientService) {
        this.authzClientService = authzClientService;
    }

    @GetMapping("${auth-center.client.path-prefix:/authz}/menus")
    public PkuseResult menus() {
        if (!StpUtil.isLogin()) {
            return PkuseResult.get(PkuseResult.CODE_NOT_LOGIN, NOT_LOGIN_MSG, null);
        }
        return PkuseResult.data(authzClientService.getMenus(StpUtil.getLoginId().toString()));
    }

    @GetMapping("${auth-center.client.path-prefix:/authz}/perms")
    public PkuseResult perms() {
        if (!StpUtil.isLogin()) {
            return PkuseResult.get(PkuseResult.CODE_NOT_LOGIN, NOT_LOGIN_MSG, null);
        }
        return PkuseResult.data(authzClientService.getPerms(StpUtil.getLoginId().toString()));
    }

    @GetMapping("${auth-center.client.path-prefix:/authz}/roles")
    public PkuseResult roles() {
        if (!StpUtil.isLogin()) {
            return PkuseResult.get(PkuseResult.CODE_NOT_LOGIN, NOT_LOGIN_MSG, null);
        }
        return PkuseResult.data(authzClientService.getRoles(StpUtil.getLoginId().toString()));
    }
}
