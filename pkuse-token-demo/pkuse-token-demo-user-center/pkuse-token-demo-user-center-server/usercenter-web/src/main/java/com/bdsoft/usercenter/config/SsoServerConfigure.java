package com.bdsoft.usercenter.config;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.spring.SpringMVCUtil;
import com.bdsoft.pkusetoken.sso.template.PkuseSsoServerTemplate;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.auth.bo.LoginResultBO;
import com.bdsoft.usercenter.common.exception.BizException;
import com.bdsoft.usercenter.auth.dto.LoginDTO;
import com.bdsoft.usercenter.user.po.UcUser;
import com.bdsoft.usercenter.auth.service.AuthService;
import com.bdsoft.usercenter.user.service.UcUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * SSO-Server（认证中心）配置
 *
 * <p> 用户中心即统一认证中心，提供以下标准 SSO 协议地址： </p>
 * <pre>
 *   /sso/auth     单点登录授权地址（Client 端重定向过来）
 *   /sso/doLogin  账号密码登录接口（登录页调用）
 *   /sso/signout  单点注销地址
 * </pre>
 */
@Configuration
public class SsoServerConfigure {

    private static final Logger log = LoggerFactory.getLogger(SsoServerConfigure.class);

    private final AuthService authService;
    private final UcUserService userService;

    /** 前端（统一登录页）地址 */
    @Value("${user-center.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public SsoServerConfigure(AuthService authService, UcUserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @Autowired
    public void configSso(PkuseSsoServerTemplate ssoServerTemplate) {

        // 1、未登录视图：重定向到前端「统一登录页」，并把原始授权地址通过 redirect 参数带回，
        //    登录成功后前端跳回 /sso/auth，由认证中心下发 ticket 重定向回 Client 端。
        ssoServerTemplate.strategy.notLoginView = () -> {
            HttpServletRequest request = SpringMVCUtil.getRequest();
            String currentUrl = request.getRequestURL().toString();
            if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
                currentUrl += "?" + request.getQueryString();
            }
            String loginUrl = frontendBaseUrl + "/login?redirect="
                    + URLEncoder.encode(currentUrl, StandardCharsets.UTF_8);
            log.info("SSO：用户未登录，跳转统一登录页 -> {}", loginUrl);
            return new ModelAndView(new RedirectView(loginUrl));
        };

        // 2、登录处理：查询数据库校验账号密码（与用户中心自身登录共用同一套逻辑：
        //    含账号锁定检查、滑块验证码策略、设备锁与登录日志）
        ssoServerTemplate.strategy.doLoginHandle = (name, pwd) -> {
            try {
                LoginDTO param = new LoginDTO();
                param.setUsername(name);
                param.setPassword(pwd);
                // 应用侧如上报设备指纹则透传（未上报时服务端按 User-Agent 生成）
                param.setDeviceId(PkuseHolder.getRequest().getParam("deviceId"));
                // 应用侧如上报设备类型（pc / mobile / mini）则透传（未上报时服务端按 User-Agent 推断）
                param.setDeviceType(PkuseHolder.getRequest().getParam("deviceType"));
                // SSO 协议登录接口由应用签名/票据保护，不校验滑块验证码（该接口面向第三方应用登录页）
                LoginResultBO result = authService.login(param, SpringMVCUtil.getRequest(), false);
                log.info("SSO：用户 [{}] 登录成功", result.getUser().getUsername());
                return PkuseResult.ok("登录成功").setData(StpUtil.getTokenValue());
            } catch (BizException e) {
                return PkuseResult.get(e.getCode(), e.getMessage(), null);
            }
        };

        // 3、为 Client 端开放 userinfo 消息：Client 可通过 SSO 协议拉取登录用户资料
        ssoServerTemplate.messageHolder.addHandle("userinfo", (template, message) -> {
            String loginId = toStringOrNull(message.get("loginId"));
            if (loginId == null) {
                return PkuseResult.error("缺少 loginId 参数");
            }
            UcUser user = userService.getByIdOrThrow(loginId);
            return PkuseResult.ok()
                    .set("id", user.getId())
                    .set("username", user.getUsername())
                    .set("nickname", user.getNickname())
                    .set("role", user.getRole());
        });
    }

    /** 登录 ID 统一转字符串（String 主键；兼容历史会话中的数字形式） */
    private static String toStringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }

}
