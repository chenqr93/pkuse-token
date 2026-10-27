package com.bdsoft.usercenter.config;

import com.bdsoft.pkusetoken.interceptor.PkuseInterceptor;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.usercenter.common.exception.BizException;
import com.bdsoft.usercenter.auth.service.AuthService;
import com.bdsoft.usercenter.user.po.UcUser;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Pkuse-Token 拦截器配置
 *
 * <pre>
 *   1、/api/** 下的接口统一要求登录（登录/登出接口除外），未登录抛出 NotLoginException，
 *      由 GlobalExceptionHandler 转换为 code=401 返回，前端跳转登录页；
 *   2、管理接口（用户管理、应用管理、日志、规则配置、工作台总览）仅系统管理员（role=admin）可访问，
 *      普通用户访问抛出 NotRoleException，转换为 code=403 返回；
 *   3、「需修改密码」拦截（规则配置 - 首次登录强制修改密码）：处于该状态的账号，
 *      除修改密码等必要接口外一律返回 code=426，前端引导先完成改密。
 * </pre>
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    /** 管理接口路径（仅系统管理员） */
    private static final String[] ADMIN_PATTERNS = {
            "/api/user/**", "/api/client/**", "/api/log/**", "/api/config/**", "/api/dashboard/overview"};

    /** 置为「需修改密码」时仍可访问的接口（登录前接口 + 改密相关） */
    private static final String[] MUST_CHANGE_PASSWORD_EXCLUDES = {
            "/api/auth/login", "/api/auth/logout",
            "/api/auth/captcha", "/api/auth/need-captcha",
            "/api/auth/public-key", "/api/auth/device/**",
            "/api/auth/me", "/api/auth/password-rule", "/api/auth/change-password"};

    private final AuthService authService;

    public SaTokenConfigure(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1、登录校验
        registry.addInterceptor(new PkuseInterceptor(handler -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                // 登录、登出、验证码、SM2 公钥、新设备验证等接口在登录前调用，无需校验
                .excludePathPatterns("/api/auth/login", "/api/auth/logout",
                        "/api/auth/captcha", "/api/auth/need-captcha",
                        "/api/auth/public-key", "/api/auth/device/**");

        // 2、管理员校验：用户管理、应用管理、日志、规则配置等维护性接口仅系统管理员可用
        registry.addInterceptor(new PkuseInterceptor(handler -> StpUtil.checkRole(UcUser.ROLE_ADMIN)))
                .addPathPatterns(ADMIN_PATTERNS);

        // 3、需修改密码校验：开启「首次登录强制修改密码」后，未改密前不放行其他接口
        registry.addInterceptor(new PkuseInterceptor(handler -> {
                    if (authService.isPasswordChangeRequired(toStringOrNull(StpUtil.getLoginIdDefaultNull()))) {
                        throw new BizException(AuthService.CODE_MUST_CHANGE_PASSWORD,
                                "请先修改初始密码后再使用系统");
                    }
                }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(MUST_CHANGE_PASSWORD_EXCLUDES);
    }

    /**
     * 跨域配置：开发时前端（vite dev server）默认通过代理访问后端，
     * 此处放开 5173 端口（vite.config 当前端口），便于前端直连调试。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
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
