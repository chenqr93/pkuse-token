package com.bdsoft.client.controller;

import com.bdsoft.client.config.SsoClientProperties;
import com.bdsoft.client.sso.LocalSessionStore;
import com.bdsoft.client.sso.NonceCache;
import com.bdsoft.client.sso.SsoRequestUtil;
import com.bdsoft.client.sso.SsoSignUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SSO 客户端4：NoSdk 模式（不依赖 Pkuse-Token 客户端 SDK，手动实现协议）
 *
 * <pre>
 *   /sso/getSsoAuthUrl   拼装认证中心授权地址（/sso/auth?client=xxx&amp;redirect=xxx）
 *   /sso/doLoginByTicket 调用认证中心 /sso/pushS（msgType=checkTicket）校验 ticket 并建立本地会话
 *   /sso/logout          调用认证中心 /sso/pushS（msgType=signout）完成单点注销
 *   /sso/myInfo          调用认证中心 /sso/pushS（msgType=userinfo）获取账号资料
 *   /sso/pushC           接收认证中心推送（校验签名，处理 logoutCall 单点注销回调）
 * </pre>
 */
@RestController
public class SsoClientController {

    private final SsoClientProperties properties;
    private final SsoSignUtil signUtil;
    private final SsoRequestUtil requestUtil;
    private final LocalSessionStore sessionStore;
    private final NonceCache nonceCache;

    public SsoClientController(SsoClientProperties properties, SsoSignUtil signUtil,
                               SsoRequestUtil requestUtil, LocalSessionStore sessionStore,
                               NonceCache nonceCache) {
        this.properties = properties;
        this.signUtil = signUtil;
        this.requestUtil = requestUtil;
        this.sessionStore = sessionStore;
        this.nonceCache = nonceCache;
    }

    /** 当前是否登录（前端调用，token 通过请求头/参数 satoken 传递） */
    @GetMapping("/sso/isLogin")
    public Map<String, Object> isLogin(HttpServletRequest request) {
        String token = readToken(request);
        Object loginId = sessionStore.getLoginId(token);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("msg", "ok");
        result.put("isLogin", loginId != null);
        result.put("loginId", loginId);
        result.put("token", token);
        return result;
    }

    /** 返回认证中心授权地址（前端跳转过去完成统一登录） */
    @GetMapping("/sso/getSsoAuthUrl")
    public Map<String, Object> getSsoAuthUrl(String clientLoginUrl) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("client", properties.getClientId());
        params.put("redirect", clientLoginUrl);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("msg", "ok");
        result.put("data", SsoRequestUtil.buildUrl(properties.authUrl(), params));
        return result;
    }

    /** 根据 ticket 登录本应用：调用认证中心 checkTicket 换取 loginId，并建立本地会话 */
    @GetMapping("/sso/doLoginByTicket")
    public Map<String, Object> doLoginByTicket(String ticket) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("ticket", ticket);
        try {
            Map<String, Object> pushResult = requestUtil.pushMessage("checkTicket", params);
            if (!isSuccess(pushResult)) {
                return error("ticket 校验失败：" + pushResult.get("msg"));
            }
            Object loginId = pushResult.get("loginId");
            if (loginId == null) {
                loginId = pushResult.get("data");
            }
            // 记录认证中心侧该会话的设备 id（单点注销时用于按设备过滤，避免误杀其它设备会话）
            String deviceId = pushResult.get("deviceId") == null ? null : String.valueOf(pushResult.get("deviceId"));
            String token = sessionStore.create(loginId, deviceId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("code", 200);
            result.put("msg", "登录成功");
            result.put("data", token);
            result.put("loginId", loginId);
            return result;
        } catch (Exception e) {
            return error("调用认证中心失败：" + e.getMessage());
        }
    }

    /** 单应用注销（只退出本应用，不通知认证中心） */
    @GetMapping("/sso/logoutByAlone")
    public Map<String, Object> logoutByAlone(HttpServletRequest request) {
        sessionStore.remove(readToken(request));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("msg", "已退出本应用");
        return result;
    }

    /** 单点注销：调用认证中心 signout 消息，随后回到 back 地址 */
    @RequestMapping("/sso/logout")
    public void logout(String back, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = readToken(request);
        Object loginId = sessionStore.getLoginId(token);
        if (loginId != null) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("loginId", String.valueOf(loginId));
            // 携带设备 id：认证中心只注销当前设备的会话（该账号其它设备/浏览器不受影响）
            String deviceId = sessionStore.getDeviceId(token);
            if (deviceId != null && !deviceId.isBlank()) {
                params.put("deviceId", deviceId);
            }
            try {
                requestUtil.pushMessage("signout", params);
            } catch (Exception e) {
                // 认证中心不可用时，至少保证本地注销
                e.printStackTrace();
            }
            sessionStore.remove(token);
        }
        response.sendRedirect(back == null || back.isBlank() ? "/" : back);
    }

    /** 获取账号资料：调用认证中心 userinfo 消息 */
    @GetMapping("/sso/myInfo")
    public Map<String, Object> myInfo(HttpServletRequest request) {
        Object loginId = sessionStore.getLoginId(readToken(request));
        if (loginId == null) {
            return error("尚未登录，无法获取");
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("loginId", String.valueOf(loginId));
        try {
            return requestUtil.pushMessage("userinfo", params);
        } catch (Exception e) {
            return error("调用认证中心失败：" + e.getMessage());
        }
    }

    /** 接收认证中心推送消息（单点注销回调等），校验签名后处理 */
    @RequestMapping("/sso/pushC")
    public Map<String, Object> pushC(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            params.put(entry.getKey(), entry.getValue()[0]);
        }

        // 校验签名（timestamp / nonce / sign）
        if (!signUtil.verifySign(params, nonceCache)) {
            return error("无效签名，拒绝应答");
        }

        String msgType = params.get("msgType");
        if ("logoutCall".equals(msgType)) {
            // 单点注销回调：清除该账号在本应用的会话（带 deviceId 时只清除该设备的会话）
            Object loginId = params.get("loginId");
            int count = sessionStore.removeByLoginId(loginId, params.get("deviceId"));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("code", 200);
            result.put("msg", "账号 loginId=" + loginId + " 在本应用注销成功（清理会话 " + count + " 个）");
            return result;
        }
        return error("未知消息类型：" + msgType);
    }

    /** 首页（直接访问后端时用于快速查看登录态） */
    @RequestMapping("/")
    public String index(HttpServletRequest request) {
        Object loginId = sessionStore.getLoginId(readToken(request));
        return "<h2>SSO 客户端4（NoSdk：不依赖 SDK，手动实现协议）</h2>"
                + "<p>当前是否登录：" + (loginId != null) + "（" + (loginId == null ? "" : loginId) + "）</p>"
                + "<p>本地会话数：" + sessionStore.size() + "</p>"
                + "<p>前端页面：<a href='http://127.0.0.1:5177/'>http://127.0.0.1:5177/</a></p>";
    }

    // ------------------------- 私有方法

    /** 读取 token：优先请求头 satoken，其次请求参数 */
    private String readToken(HttpServletRequest request) {
        String token = request.getHeader("satoken");
        if (token == null || token.isBlank()) {
            token = request.getParameter("satoken");
        }
        return token;
    }

    private static boolean isSuccess(Map<String, Object> result) {
        Object code = result.get("code");
        return code instanceof Number number && number.intValue() == 200;
    }

    private static Map<String, Object> error(String msg) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 500);
        result.put("msg", msg);
        result.put("data", null);
        return result;
    }

}
