package com.bdsoft.client.controller;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoClientConfig;
import com.bdsoft.pkusetoken.sso.message.PkuseSsoMessage;
import com.bdsoft.pkusetoken.sso.template.PkuseSsoClientUtil;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * SSO 客户端1：模式一对接（同域 + 同 Redis）
 *
 * <pre>
 *   登录：/sso/login?back=xxx
 *         —— 未登录时 302 到认证中心 /sso/auth?mode=simple（同域 Cookie + 同 Redis，无需 ticket）；
 *         —— 已登录（认证中心登录后 Cookie 与会话共享）时 302 回 back 地址。
 *   注销：/sso/logoutByAlone（仅本应用）  /sso/logout?back=xxx（单点注销，通知认证中心）
 *   资料：/sso/myInfo（向认证中心推送 userinfo 消息获取）
 * </pre>
 */
@RestController
public class SsoClientController {

    /** 当前是否登录（前端调用） */
    @GetMapping("/sso/isLogin")
    public PkuseResult isLogin() {
        return PkuseResult.ok()
                .set("isLogin", StpUtil.isLogin())
                .set("loginId", StpUtil.getLoginIdDefaultNull())
                .set("token", StpUtil.getTokenValue());
    }

    /**
     * 单点登录入口（模式一）
     *
     * <p> 同域 + 同 Redis：认证中心登录后 Cookie 与本应用共享、会话数据共享， </p>
     * <p> 因此使用 mode=simple 直接跳回，无需 ticket 交换。 </p>
     */
    @RequestMapping("/sso/login")
    public Object login(String back, HttpServletResponse response) throws IOException {
        String backUrl = (back == null || back.isBlank()) ? "/" : back;

        // 已登录（Cookie 与 Redis 会话共享，可直接判断）→ 回到目标地址
        if (StpUtil.isLogin()) {
            response.sendRedirect(backUrl);
            return null;
        }

        // 未登录 → 跳认证中心（mode=simple）
        String clientLoginUrl = PkuseHolder.getRequest().getUrl()
                + "?back=" + PkuseFoxUtil.encodeUrl(backUrl);
        PkuseSsoClientConfig cfg = PkuseSsoManager.getClientConfig();
        String serverAuthUrl = cfg.splicingAuthUrl()
                + "?mode=simple"
                + "&client=" + cfg.getClient()
                + "&redirect=" + PkuseFoxUtil.encodeUrl(clientLoginUrl);
        response.sendRedirect(serverAuthUrl);
        return null;
    }

    /** 单应用注销（只退出本应用，不通知认证中心） */
    @RequestMapping("/sso/logoutByAlone")
    public PkuseResult logoutByAlone() {
        StpUtil.logout();
        return PkuseResult.ok("已退出本应用");
    }

    /**
     * 单点注销：浏览器跳转认证中心注销地址，由认证中心完成注销并回调 back
     *
     * <p> 模式一/二共用同一个 Redis，认证中心注销后本应用会话即刻失效。 </p>
     */
    @RequestMapping("/sso/logout")
    public void logout(String back, String singleDeviceIdLogout, HttpServletResponse response) throws IOException {
        String backUrl = (back == null || back.isBlank()) ? "/" : back;
        PkuseSsoClientConfig cfg = PkuseSsoManager.getClientConfig();
        String signoutUrl = cfg.splicingSignoutUrl()
                + "?back=" + PkuseFoxUtil.encodeUrl(backUrl)
                + "&singleDeviceIdLogout=" + (singleDeviceIdLogout == null ? "true" : singleDeviceIdLogout);
        response.sendRedirect(signoutUrl);
    }

    /** 获取账号资料：本应用后端 → 认证中心 userinfo 消息 */
    @RequestMapping("/sso/myInfo")
    public PkuseResult myInfo() {
        if (!StpUtil.isLogin()) {
            return PkuseResult.error("尚未登录，无法获取");
        }
        PkuseSsoMessage message = new PkuseSsoMessage();
        message.setType("userinfo");
        message.set("loginId", StpUtil.getLoginId());
        return PkuseSsoClientUtil.pushMessageAsSaResult(message);
    }

    /** 首页（直接访问后端时用于快速查看登录态） */
    @RequestMapping("/")
    public String index() {
        return "<h2>SSO 客户端1（模式一：同域 + 同 Redis）</h2>"
                + "<p>当前是否登录：" + StpUtil.isLogin() + "（" + StpUtil.getLoginId("") + "）</p>"
                + "<p>前端页面：<a href='http://localhost:5174/'>http://localhost:5174/</a></p>";
    }

}
