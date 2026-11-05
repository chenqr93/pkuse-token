package com.bdsoft.authcenter.client;

import com.bdsoft.pkusetoken.stp.StpInterface;

import java.util.List;

/**
 * Pkuse-Token StpInterface 适配：权限码与角色编码均由授权中心按本应用分发
 *
 * <p> 应用侧 @PkuseCheckPermission("sys:user:del") / StpUtil.checkPermission(...) 直接可用； </p>
 * <p> @PkuseCheckRole("admin") / StpUtil.checkRole(...) 亦可直接可用 </p>
 * <p>（角色来自授权中心 /open/authz/roles，「用户在本应用的角色」与用户中心平台角色无关）。 </p>
 */
public class PkuseStpInterfaceAdapter implements StpInterface {

    private final AuthzClientService authzClientService;

    public PkuseStpInterfaceAdapter(AuthzClientService authzClientService) {
        this.authzClientService = authzClientService;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return authzClientService.getPerms(loginId == null ? null : loginId.toString());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return authzClientService.getRoles(loginId == null ? null : loginId.toString());
    }
}
