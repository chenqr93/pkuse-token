package com.bdsoft.authcenter.config;

import com.bdsoft.authcenter.common.config.AuthCenterProperties;
import com.bdsoft.pkusetoken.interceptor.PkuseInterceptor;
import com.bdsoft.pkusetoken.stp.StpUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Pkuse-Token 拦截器配置
 *
 * <pre>
 *   1、/api/authz/** 下的控制台接口统一要求登录 + 系统管理员（role=admin，取自登录时
 *      缓存的用户中心平台角色），未登录返回 401、非管理员返回 403；
 *   2、/open/authz/**（签名开放接口）与 /sso/**（SSO 协议）放行，由各自机制自校验；
 *   3、启动时校验 SSO 模式与 token-name 的组合合法性（防 Cookie 覆盖陷阱，见设计文档风险 2）。
 * </pre>
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SaTokenConfigure.class);

    /** 平台管理员角色（与用户中心 uc_user.role 一致） */
    private static final String ROLE_ADMIN = "admin";

    /** simple 模式必须沿用的用户中心 Cookie 名（共享 Cookie 的前提） */
    private static final String SHARED_TOKEN_NAME = "satoken";

    private final AuthCenterProperties properties;
    private final Environment environment;

    public SaTokenConfigure(AuthCenterProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    /** 启动校验：token-name 必须与 SSO 模式匹配，不匹配快速失败（从 Environment 读取，避免早于 SDK 注入） */
    @PostConstruct
    public void checkModeAndTokenName() {
        String tokenName = environment.getProperty("sa-token.token-name", SHARED_TOKEN_NAME);
        if (properties.getSso().isTicketMode()) {
            if (SHARED_TOKEN_NAME.equals(tokenName)) {
                throw new IllegalStateException("ticket 模式下 token-name 必须独立命名（如 authcenter-token）："
                        + "沿用 satoken 会在同域部署时覆盖用户中心的 Cookie，导致用户中心会话丢失");
            }
        } else if (!SHARED_TOKEN_NAME.equals(tokenName)) {
            throw new IllegalStateException("simple 模式下 token-name 必须沿用 satoken（与用户中心共享 Cookie 的前提），"
                    + "当前为：" + tokenName);
        }
        log.info("SSO 对接模式：{}（token-name={}）",
                properties.getSso().getIntegrationMode(), tokenName);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1、登录校验：控制台接口
        registry.addInterceptor(new PkuseInterceptor(handler -> StpUtil.checkLogin()))
                .addPathPatterns("/api/authz/**");

        // 2、管理员校验：授权中心控制台仅系统管理员可用
        registry.addInterceptor(new PkuseInterceptor(handler -> StpUtil.checkRole(ROLE_ADMIN)))
                .addPathPatterns("/api/authz/**");
    }

    /**
     * 跨域配置：开发时前端（vite dev server）默认通过代理访问后端，
     * 此处放开 5180 端口（vite.config 当前端口），便于前端直连调试。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:5180", "http://127.0.0.1:5180")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
