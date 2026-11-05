package com.bdsoft.authcenter.sso.controller;

import com.bdsoft.authcenter.common.config.AuthCenterProperties;
import com.bdsoft.authcenter.usercenter.bo.UserInfo;
import com.bdsoft.authcenter.usercenter.support.UserCenterClient;
import com.bdsoft.authcenter.usercenter.support.UserInfoCache;
import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoClientConfig;
import com.bdsoft.pkusetoken.sso.model.PkuseCheckTicketResult;
import com.bdsoft.pkusetoken.sso.processor.PkuseSsoClientProcessor;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLoginParameter;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SSO 客户端控制器：授权中心作为用户中心的 SSO Client（双模式，配置 auth-center.sso.integration-mode）
 *
 * <pre>
 *   /sso/isLogin                 登录态查询（含缓存的用户信息，纯身份不含角色；
 *                                角色判断走 /api/authz/my-roles，本地 RBAC）
 *   /sso/login?back=xxx          登录入口：已登录直接回跳；未登录 302 到用户中心 /sso/auth
 *                                （simple 模式 mode=simple 免 ticket；ticket 模式 mode=ticket）
 *   /sso/loginByTicket?ticket=   ticket 模式回跳：checkTicket 校验 → 本地登录 → 缓存用户信息 → 回跳
 *   /sso/logout?back=xxx         单点注销（跳用户中心注销地址）
 *   /sso/*                       SDK 协议入口（logoutCall / pushC 等推送消息）
 *   /sso/myInfo                  userinfo 消息通道（演示用）
 * </pre>
 */
@RestController
public class SsoClientController {

    private static final Logger log = LoggerFactory.getLogger(SsoClientController.class);

    @Resource
    private AuthCenterProperties properties;
    @Resource
    private UserCenterClient userCenterClient;
    @Resource
    private UserInfoCache userInfoCache;


    /** SSO 协议入口（SDK）：logoutCall、pushC 等 */
    @RequestMapping("/sso/*")
    public Object ssoRequest() {
        return PkuseSsoClientProcessor.instance.dister();
    }

    /** 当前是否登录（前端调用，token 通过请求头 authcenter-token / Cookie 传递；data={isLogin,loginId,token,ssoMode,user}） */
    @GetMapping("/sso/isLogin")
    public PkuseResult isLogin() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("isLogin", StpUtil.isLogin());
        data.put("loginId", StpUtil.getLoginIdDefaultNull());
        data.put("token", StpUtil.getTokenValue());
        data.put("ssoMode", properties.getSso().isTicketMode()
                ? AuthCenterProperties.Sso.MODE_TICKET : AuthCenterProperties.Sso.MODE_SIMPLE);
        if (StpUtil.isLogin()) {
            UserInfo info = userInfoCache.getOrFetch(StpUtil.getLoginId().toString());
            if (info != null) {
                data.put("user", info);
            }
        }
        return PkuseResult.ok().setData(data);
    }

    /**
     * 登录入口（双模式）
     *
     * <p> 已登录：补缓存用户信息后 302 回 back；未登录：302 到用户中心授权地址 </p>
     * <p>（simple：mode=simple，回跳本入口即可；ticket：mode=ticket，回跳 /sso/loginByTicket）。 </p>
     */
    @RequestMapping("/sso/login")
    public void login(String back, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String backUrl = normalizeBack(back);
        if (StpUtil.isLogin()) {
            cacheUserInfoQuietly();
            response.sendRedirect(backUrl);
            return;
        }
        boolean ticketMode = properties.getSso().isTicketMode();
        String callbackUrl;
        if (ticketMode) {
            callbackUrl = requestBaseUrl(request) + "/sso/loginByTicket?back=" + PkuseFoxUtil.encodeUrl(backUrl);
        } else {
            // simple 模式：回跳本入口，登录后共享会话即可识别
            callbackUrl = PkuseHolder.getRequest().getUrl() + "?back=" + PkuseFoxUtil.encodeUrl(backUrl);
        }
        PkuseSsoClientConfig cfg = PkuseSsoManager.getClientConfig();
        String serverAuthUrl = cfg.splicingAuthUrl()
                + "?mode=" + (ticketMode ? "ticket" : "simple")
                + "&client=" + cfg.getClient()
                + "&redirect=" + PkuseFoxUtil.encodeUrl(callbackUrl);
        response.sendRedirect(serverAuthUrl);
    }

    /**
     * ticket 模式回跳：校验 ticket 换取本地会话，缓存用户信息后回跳
     */
    @RequestMapping("/sso/loginByTicket")
    public void loginByTicket(String ticket, String back, HttpServletResponse response) throws IOException {
        String backUrl = normalizeBack(back);
        PkuseCheckTicketResult ctr = PkuseSsoClientProcessor.instance.checkTicket(ticket, "/sso/loginByTicket");
        StpUtil.login(ctr.loginId, new PkuseLoginParameter()
                .setTimeout(ctr.remainTokenTimeout)
                .setDeviceId(ctr.deviceId));
        cacheUserInfoQuietly();
        log.info("ticket 登录成功：loginId={}", ctr.loginId);
        response.sendRedirect(backUrl);
    }

    /** 单点注销：浏览器跳用户中心注销地址（ticket 模式下认证中心会推送 logoutCall 清本端会话） */
    @RequestMapping("/sso/logout")
    public void logout(String back, HttpServletResponse response) throws IOException {
        String backUrl = normalizeBack(back);
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId != null) {
            userInfoCache.evict(loginId.toString());
        }
        PkuseSsoClientConfig cfg = PkuseSsoManager.getClientConfig();
        response.sendRedirect(cfg.splicingSignoutUrl()
                + "?back=" + PkuseFoxUtil.encodeUrl(backUrl)
                + "&singleDeviceIdLogout=true");
    }

    /** 获取账号资料：userinfo 消息通道（SDK 签名调用，与本地缓存互为印证；统一信封返回） */
    @GetMapping("/sso/myInfo")
    public PkuseResult myInfo() {
        if (!StpUtil.isLogin()) {
            return PkuseResult.get(PkuseResult.CODE_NOT_LOGIN, "尚未登录，无法获取", null);
        }
        return PkuseResult.ok().setData(userCenterClient.fetchMe());
    }

    /** 登录后缓存用户信息（失败不影响登录主流程） */
    private void cacheUserInfoQuietly() {
        try {
            UserInfo info = userCenterClient.fetchMe();
            if (info != null) {
                userInfoCache.cache(info);
            }
        } catch (Exception e) {
            log.warn("登录后缓存用户信息失败（不影响登录）：{}", e.getMessage());
        }
    }

    /** back 地址规范化（空或非法时回前端首页） */
    private static String normalizeBack(String back) {
        if (back == null || back.isBlank() || !back.startsWith("http") && !back.startsWith("/")) {
            return "/";
        }
        return back;
    }

    /** 当前请求的根地址（scheme://host:port，用于拼装 ticket 回跳地址） */
    private static String requestBaseUrl(HttpServletRequest request) {
        URI uri = URI.create(PkuseHolder.getRequest().getUrl());
        // 带上 servlet context-path（子路径部署时为 /authcenter，本地开发为空）
        return uri.getScheme() + "://" + uri.getRawAuthority() + request.getContextPath();
    }
}
