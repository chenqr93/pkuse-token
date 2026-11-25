package com.bdsoft.authcenter.controller.authz;

import com.bdsoft.authcenter.service.authz.AcMenuService;
import com.bdsoft.authcenter.service.authz.AcRoleService;
import com.bdsoft.authcenter.service.authz.AcUserRoleService;
import com.bdsoft.authcenter.support.authz.AppRegistryCache;
import com.bdsoft.authcenter.vo.authz.OverviewVO;
import com.bdsoft.authcenter.common.config.AuthCenterProperties;
import com.bdsoft.authcenter.bo.usercenter.UserInfo;
import com.bdsoft.authcenter.support.usercenter.UserInfoCache;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;

/**
 * 概览与当前用户 Controller（统计全部本地查询，不依赖用户中心）
 */
@RestController
@RequestMapping("/api/authz")
public class OverviewController {

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
    private UserInfoCache userInfoCache;
    @Resource
    private AuthCenterProperties properties;


    /** 概览统计：应用数（注册表启用）/ 菜单数 / 角色数 / 已授权用户数 + 当前 SSO 模式 */
    @GetMapping("/overview")
    public PkuseResult overview() {
        OverviewVO vo = new OverviewVO();
        vo.setAppCount(registryCache.countEnabled());
        vo.setMenuCount(menuService.count());
        vo.setRoleCount(roleService.count());
        vo.setUserCount(userRoleService.countDistinctUsers());
        vo.setSsoMode(properties.getSso().isTicketMode()
                ? AuthCenterProperties.Sso.MODE_TICKET : AuthCenterProperties.Sso.MODE_SIMPLE);
        vo.setLastRefreshTime(registryCache.lastRefreshTime() == null
                ? null : TIME_FORMATTER.format(registryCache.lastRefreshTime()));
        return PkuseResult.ok().setData(vo);
    }

    /** 当前登录用户（本地会话缓存；未缓存时回源用户中心；纯身份不含角色） */
    @GetMapping("/me")
    public PkuseResult me() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return PkuseResult.get(PkuseResult.CODE_NOT_LOGIN, "未登录", null);
        }
        UserInfo info = userInfoCache.getOrFetch(loginId.toString());
        if (info == null) {
            return PkuseResult.error("用户信息获取失败（用户中心不可用），请稍后重试或重新登录");
        }
        return PkuseResult.ok().setData(info);
    }

    /**
     * 当前登录用户在控制台的角色编码集合（本地 RBAC，client=本应用）
     *
     * <p> 用户信息与角色拆分后，前端管理端的角色判断（如 admin 菜单显隐）改走本接口； </p>
     * <p> 数据源为本地 ac_user_role + ac_role（种子见 db/upgrade-v3-console-role.sql）。 </p>
     */
    @GetMapping("/my-roles")
    public PkuseResult myRoles() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return PkuseResult.get(PkuseResult.CODE_NOT_LOGIN, "未登录", null);
        }
        return PkuseResult.ok().setData(StpUtil.getRoleList());
    }
}
