package com.bdsoft.authcenter.config;

import com.bdsoft.authcenter.common.config.AuthCenterProperties;
import com.bdsoft.pkusetoken.interceptor.PkuseInterceptor;
import com.bdsoft.pkusetoken.stp.StpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Pkuse-Token 拦截器配置（simple 共享会话模式）
 *
 * <pre>
 *   1、/api/authz/** 下的控制台接口统一要求登录 + 系统管理员（role=admin，取自登录时
 *      缓存的用户中心平台角色），未登录返回 401、非管理员返回 403；
 *   2、/open/authz/**（签名开放接口）与 /sso/**（SSO 协议）放行，由各自机制自校验；
 *   3、接口文档端点（/v3/api-docs、/doc.html、/swagger-ui）放行（非 dev 环境在 yml 关闭文档）；
 *   4、dev 免登录（auth-center.dev-auth.enabled，仅 dev profile 配置开启）：拦截器自动注入
 *      固定测试身份（内置管理员 1），前后端联调不再依赖用户中心登录链路。
 * </pre>
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SaTokenConfigure.class);

    /** 平台管理员角色（与用户中心 uc_user.role 一致） */
    private static final String ROLE_ADMIN = "admin";

    /** 接口文档相关端点（登录拦截放行） */
    private static final String[] DOC_ENDPOINTS = {
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/doc.html", "/webjars/**"
    };

    private final AuthCenterProperties properties;

    public SaTokenConfigure(AuthCenterProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 0、dev 免登录（仅 dev profile 配置开启）：最先注册，未登录自动注入内置管理员身份
        if (properties.getDevAuth().isEnabled()) {
            registry.addInterceptor(new PkuseInterceptor(handler -> {
                if (!StpUtil.isLogin()) {
                    StpUtil.login(properties.getDevAuth().getLoginId());
                }
            }))
                    .addPathPatterns("/api/**")
                    .order(0);
            log.warn("【开发免登录】已启用：/api/authz/** 请求自动以 {} 身份登录（严禁用于非 dev 环境）",
                    properties.getDevAuth().getLoginId());
        }

        // 1、登录校验：控制台接口（接口文档端点放行）
        registry.addInterceptor(new PkuseInterceptor(handler -> StpUtil.checkLogin()))
                .addPathPatterns("/api/authz/**")
                .excludePathPatterns(DOC_ENDPOINTS);

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
