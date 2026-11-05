package com.bdsoft.authcenter.open.controller;

import com.bdsoft.authcenter.authz.bo.RegistryApp;
import com.bdsoft.authcenter.authz.support.AppRegistryCache;
import com.bdsoft.authcenter.authz.support.AuthzAssembler;
import com.bdsoft.authcenter.authz.support.OpenSignVerifier;
import com.bdsoft.authcenter.authz.vo.MenuTreeVO;
import com.bdsoft.pkusetoken.util.PkuseResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开放分发接口（/open/authz/**）：各客户端应用后端拉取「用户在本应用的菜单树 / 权限码集合」
 *
 * <p> 鉴权：MD5 参数签名（client、loginId、timestamp、nonce 参与签名，密钥为该应用在注册表缓存中 </p>
 * <p> 的最新 secret_key）；重放/过期/密钥错误/应用未启用统一返回 403，不泄露差异细节。 </p>
 * <p> 验签失败或遇到未知应用时自动刷新注册表缓存并重试一次（消除密钥轮换窗口）。 </p>
 * <p> 全链路只读本地库与缓存，不依赖用户中心可用性。 </p>
 *
 * <pre>
 *   GET /open/authz/menus?client=sso-client3&loginId=10002&timestamp=..&nonce=..&sign=..
 *   GET /open/authz/perms?client=sso-client3&loginId=10002&timestamp=..&nonce=..&sign=..
 *   GET /open/authz/roles?client=sso-client3&loginId=10002&timestamp=..&nonce=..&sign=..
 * </pre>
 */
@RestController
@RequestMapping("/open/authz")
public class OpenAuthzController {

    @Resource
    private AppRegistryCache registryCache;
    @Resource
    private OpenSignVerifier signVerifier;
    @Resource
    private AuthzAssembler authzAssembler;


    /** 用户可见菜单树（启用+显示的目录/页面，多角色并集；无角色返回空数组） */
    @GetMapping("/menus")
    public PkuseResult menus(String client, String loginId, String timestamp, String nonce, String sign) {
        RegistryApp app = verifySign(client, loginId, timestamp, nonce, sign);
        if (app == null) {
            return denied();
        }
        List<MenuTreeVO> menus = authzAssembler.distributeMenus(client, loginId);
        return PkuseResult.ok().setData(menus);
    }

    /** 用户权限码集合（启用菜单上非空 perms 并集，含按钮与隐藏页面） */
    @GetMapping("/perms")
    public PkuseResult perms(String client, String loginId, String timestamp, String nonce, String sign) {
        RegistryApp app = verifySign(client, loginId, timestamp, nonce, sign);
        if (app == null) {
            return denied();
        }
        return PkuseResult.ok().setData(authzAssembler.distributePerms(client, loginId));
    }

    /** 用户角色编码集合（绑定且启用的角色，按角色编码排序；无绑定返回空数组） */
    @GetMapping("/roles")
    public PkuseResult roles(String client, String loginId, String timestamp, String nonce, String sign) {
        RegistryApp app = verifySign(client, loginId, timestamp, nonce, sign);
        if (app == null) {
            return denied();
        }
        return PkuseResult.ok().setData(authzAssembler.distributeRoles(client, loginId));
    }

    /**
     * 验签：应用需在注册表且启用；失败时刷新注册表重试一次（密钥轮换即时收敛）
     *
     * @return 校验通过的应用档案；失败返回 null
     */
    private RegistryApp verifySign(String client, String loginId, String timestamp, String nonce, String sign) {
        if (checkOnce(client, loginId, timestamp, nonce, sign)) {
            return registryCache.get(client);
        }
        // 刷新注册表后重试一次（密钥可能已在用户中心轮换）
        registryCache.refreshQuietly();
        if (checkOnce(client, loginId, timestamp, nonce, sign)) {
            return registryCache.get(client);
        }
        return null;
    }

    private boolean checkOnce(String client, String loginId, String timestamp, String nonce, String sign) {
        RegistryApp app = registryCache.get(client);
        if (app == null || !Integer.valueOf(RegistryApp.STATUS_ENABLED).equals(app.getStatus())) {
            return false;
        }
        return signVerifier.verify(client, loginId, timestamp, nonce, sign, app.getSecretKey());
    }

    private static PkuseResult denied() {
        return PkuseResult.get(PkuseResult.CODE_NOT_PERMISSION, "签名校验失败（检查 client/loginId/timestamp/nonce/sign 与密钥）", null);
    }
}
