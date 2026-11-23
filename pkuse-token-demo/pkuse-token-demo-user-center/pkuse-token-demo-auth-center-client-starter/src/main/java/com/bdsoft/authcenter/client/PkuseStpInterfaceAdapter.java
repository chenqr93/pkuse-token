package com.bdsoft.authcenter.client;

import com.bdsoft.pkusetoken.stp.StpInterface;

import java.util.Collections;
import java.util.List;

/**
 * Pkuse-Token StpInterface 适配：getPermissionList 返回授权中心分发的权限码
 *
 * <p> 应用侧 @PkuseCheckPermission("sys:user:del") / StpUtil.checkPermission(...) 直接可用； </p>
 * <p> 角色维度（getRoleList）留给应用自身实现（starter 不越界，返回空集合）。 </p>
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
        return Collections.emptyList();
    }
}
