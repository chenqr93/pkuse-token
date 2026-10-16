package com.bdsoft.usercenter.auth.controller;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.sso.processor.PkuseSsoServerProcessor;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.user.po.UcUser;
import com.bdsoft.usercenter.auth.service.AuthService;
import com.bdsoft.usercenter.authz.service.UcUserClientService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SSO-Server 端协议入口（统一登录认证中心）
 *
 * <pre>
 *   http://localhost:8091/sso/auth      单点登录授权地址（Client 端重定向过来）
 *   http://localhost:8091/sso/doLogin   账号密码登录接口，接受参数：name、pwd
 *   http://localhost:8091/sso/signout   单点注销地址（is-slo=true 时可用）
 *   http://localhost:8091/sso/pushS     接收 Client 端推送的消息
 * </pre>
 *
 * <p>
 * 相关策略（未登录视图、登录处理、消息处理器）在 {@link com.bdsoft.usercenter.config.SsoServerConfigure} 中配置；
 * 应用（Client）名单由「应用管理」维护，见 {@link com.bdsoft.usercenter.client.service.SsoClientRegistry}。
 * </p>
 *
 * <p> 授权控制：普通用户只能进入已授权的应用（系统管理员不受限）。 </p>
 */
@RestController
public class SsoServerController {

    /** SSO-Server 端：授权地址 */
    private static final String API_SSO_AUTH = "/sso/auth";

    @Resource
    private AuthService authService;

    @Resource
    private UcUserClientService userClientService;

    @RequestMapping("/sso/*")
    public Object ssoRequest() {
        PkuseRequest req = PkuseHolder.getRequest();
        // 已登录用户访问授权地址时，校验其是否已获得该应用的访问授权
        if (req.isPath(API_SSO_AUTH) && StpUtil.isLogin() && !canAccessClient(req.getParam("client"))) {
            return PkuseResult.error("您没有该应用的访问权限，请联系管理员开通");
        }
        return PkuseSsoServerProcessor.instance.dister();
    }

    /** 系统管理员可访问全部应用；普通用户需已授权 */
    private boolean canAccessClient(String clientId) {
        if (PkuseFoxUtil.isEmpty(clientId)) {
            return true;
        }
        UcUser user = authService.currentUser();
        return UcUser.ROLE_ADMIN.equals(user.getRole()) || userClientService.isAuthorized(user.getId(), clientId);
    }

}
