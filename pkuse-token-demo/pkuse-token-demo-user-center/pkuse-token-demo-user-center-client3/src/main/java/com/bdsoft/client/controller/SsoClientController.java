package com.bdsoft.client.controller;

import com.bdsoft.pkusetoken.sso.message.PkuseSsoMessage;
import com.bdsoft.pkusetoken.sso.model.PkuseCheckTicketResult;
import com.bdsoft.pkusetoken.sso.processor.PkuseSsoClientProcessor;
import com.bdsoft.pkusetoken.sso.template.PkuseSsoClientUtil;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLoginParameter;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SSO 客户端3：模式三对接（跨域 + 跨 Redis）
 *
 * <pre>
 *   /sso/login?back=xxx   应用端登录地址（SDK 处理：未登录跳认证中心，带 ticket 回跳后走 HTTP 校验）
 *   /sso/logout?back=xxx  单点注销（SDK 处理）
 *   /sso/pushC            接收认证中心推送消息（SDK 处理，单点注销通知）
 *   /sso/logoutCall       单点注销回调（SDK 处理）
 * </pre>
 *
 * <p> 与模式二的区别：本应用使用自己的 Redis，ticket 无法直连 Redis 校验， </p>
 * <p> 改用 HTTP 接口向认证中心校验（sa-token.sso-client.is-http=true）。 </p>
 */
@RestController
public class SsoClientController {

    /** SSO 协议入口（SDK）：/sso/login、/sso/logout、/sso/logoutCall、/sso/pushC */
    @RequestMapping("/sso/*")
    public Object ssoRequest() {
        return PkuseSsoClientProcessor.instance.dister();
    }

    /** 当前是否登录（前端调用，token 通过请求头 satoken 传递） */
    @GetMapping("/sso/isLogin")
    public PkuseResult isLogin() {
        return PkuseResult.ok()
                .set("isLogin", StpUtil.isLogin())
                .set("loginId", StpUtil.getLoginIdDefaultNull())
                .set("token", StpUtil.getTokenValue());
    }

    /** 返回认证中心授权地址（前端跳转过去完成统一登录） */
    @GetMapping("/sso/getSsoAuthUrl")
    public PkuseResult getSsoAuthUrl(String clientLoginUrl) {
        return PkuseResult.ok().setData(PkuseSsoClientUtil.buildServerAuthUrl(clientLoginUrl, ""));
    }

    /**
     * 根据 ticket 登录本应用（模式三：HTTP 接口向认证中心校验 ticket）
     *
     * <p> 传入当前路由，用于向认证中心注册单点注销回调地址（/sso/logoutCall）。 </p>
     */
    @GetMapping("/sso/doLoginByTicket")
    public PkuseResult doLoginByTicket(String ticket) {
        PkuseCheckTicketResult ctr = PkuseSsoClientProcessor.instance.checkTicket(ticket, "/sso/doLoginByTicket");
        StpUtil.login(ctr.loginId, new PkuseLoginParameter()
                .setTimeout(ctr.remainTokenTimeout)
                .setDeviceId(ctr.deviceId));
        return PkuseResult.ok("登录成功").setData(StpUtil.getTokenValue());
    }

    /** 单应用注销（只退出本应用） */
    @GetMapping("/sso/logoutByAlone")
    public PkuseResult logoutByAlone() {
        StpUtil.logout();
        return PkuseResult.ok("已退出本应用");
    }

    /** 获取账号资料：本应用后端 → 认证中心 userinfo 消息 */
    @GetMapping("/sso/myInfo")
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
        return "<h2>SSO 客户端3（模式三：跨域 + 跨 Redis）</h2>"
                + "<p>当前是否登录：" + StpUtil.isLogin() + "（" + StpUtil.getLoginId("") + "）</p>"
                + "<p>前端页面：<a href='http://127.0.0.1:5176/'>http://127.0.0.1:5176/</a></p>";
    }

}
