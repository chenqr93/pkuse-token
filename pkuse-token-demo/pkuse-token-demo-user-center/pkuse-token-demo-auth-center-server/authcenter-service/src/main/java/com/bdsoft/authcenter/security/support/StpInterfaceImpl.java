package com.bdsoft.authcenter.security.support;

import com.bdsoft.authcenter.authz.support.AuthzAssembler;
import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import com.bdsoft.pkusetoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 权限数据源（Pkuse-Token StpInterface 实现）
 *
 * <p> 授权中心控制台自身也是一个 client（sa-token.sso-client.client，默认 auth-center）， </p>
 * <p> 管理端角色走本地 RBAC：取「用户在本控制台 client 下绑定的启用角色编码」 </p>
 * <p>（ac_user_role + ac_role，种子见 db/upgrade-v3-console-role.sql），admin 可访问全部管理接口。 </p>
 * <p> 用户信息通道（userinfo / /api/auth/me）不再携带角色，与本类无关。 </p>
 * <p> 权限码维度（getPermissionList）控制台暂不使用，返回空集合——各应用内的权限码 </p>
 * <p> 由授权中心分发接口提供、由各应用自己的 StpInterface 消费，与本类无关。 </p>
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final AuthzAssembler authzAssembler;

    public StpInterfaceImpl(AuthzAssembler authzAssembler) {
        this.authzAssembler = authzAssembler;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String userId = loginId == null ? null : loginId.toString();
        if (userId == null || userId.isBlank()) {
            return Collections.emptyList();
        }
        // 控制台角色自管理：本应用 client 维度的本地 RBAC 查询（与 /open/authz/roles 分发同源）
        return authzAssembler.distributeRoles(consoleClientId(), userId);
    }

    /** 控制台自身的 client 标识（即本应用在用户中心登记的 SSO client，默认 auth-center） */
    private static String consoleClientId() {
        return PkuseSsoManager.getClientConfig().getClient();
    }
}
