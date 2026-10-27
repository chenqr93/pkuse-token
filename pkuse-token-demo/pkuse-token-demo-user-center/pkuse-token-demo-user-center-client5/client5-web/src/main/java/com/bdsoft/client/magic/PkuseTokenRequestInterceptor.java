package com.bdsoft.client.magic;

import com.bdsoft.magicapi.core.context.RequestEntity;
import com.bdsoft.magicapi.core.interceptor.AuthorizationInterceptor;
import com.bdsoft.magicapi.core.interceptor.RequestInterceptor;
import com.bdsoft.magicapi.core.model.JsonBean;
import com.bdsoft.magicapi.script.MagicScriptContext;
import com.bdsoft.pkusetoken.stp.StpUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

/**
 * magic-api 运行时接口鉴权拦截器（自定义拦截器 × Pkuse-Token）
 *
 * <p> 拦截器约定（官方 demo）：preHandle 返回非 null 时直接把该对象返回到页面（不包裹 JSON、不走 ResultProvider），
 * 返回 null 时继续执行后续代码。本拦截器是所有 magic 接口的唯一鉴权入口，统一校验 pkuse-token 会话： </p>
 *
 * <pre>
 *   1. CORS 预检（OPTIONS 不携带 token）与白名单（magic-auth.anonymous-paths，Ant 通配）直接放行；
 *   2. 编辑器「测试」请求（isRequestedFromTest）携带的是编辑器会话 Magic-Token 头，
 *      交由 {@link AuthorizationInterceptor} 校验（builtin / pkuse-token 两种编辑器模式都兼容）；
 *   3. 普通请求走 pkuse-token 鉴权：StpUtil.isLogin()，
 *      token 自动从 satoken 请求头 / 参数 / Cookie 读取（与前端 api.js 的约定一致）；
 *   4. 鉴权通过后向脚本上下文注入 loginId / token 变量，magic 脚本中可直接引用获取当前调用方。
 * </pre>
 *
 * <p> 调用方体系（所有令牌都是本应用 db7 中的 pkuse-token 会话，区别只在令牌如何获得）： </p>
 * <pre>
 *   编辑器用户        —— AuthorizationInterceptor 登录（builtin 静态账号 / pkuse-token 认证中心账号）
 *   法治中台内部应用  —— 用户级：统一登录（SSO）后携带 satoken；应用级：/open/token 签名换取令牌
 *   外部第三方应用    —— /open/token 签名换取令牌（见 OpenTokenController）
 * </pre>
 * <p> 脚本中通过 loginId 前缀区分调用方：普通用户为用户 ID（如 10001）、开放调用方为 open:{client}。 </p>
 *
 * <p> 本类也支持 preHandle(ApiInfo, MagicScriptContext, request, response) 等重载，按需自行选择，
 * 这里选用 RequestEntity 重载：可同时拿到请求方法、请求头与「是否来自编辑器测试」标记。 </p>
 */
@Component
@Order(1) // 拦截器顺序：鉴权拦截器最先执行
public class PkuseTokenRequestInterceptor implements RequestInterceptor {

    /** 编辑器会话的请求头（magic-api 约定，登录后由编辑器自动携带） */
    public static final String MAGIC_TOKEN_HEADER = "Magic-Token";

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final MagicAuthProperties properties;
    /** 编辑器鉴权实现（builtin 模式=框架默认静态账号；pkuse-token 模式=PkuseTokenAuthorizationInterceptor） */
    private final ObjectProvider<AuthorizationInterceptor> authorizationInterceptorProvider;

    public PkuseTokenRequestInterceptor(MagicAuthProperties properties,
                                        ObjectProvider<AuthorizationInterceptor> authorizationInterceptorProvider) {
        this.properties = properties;
        this.authorizationInterceptorProvider = authorizationInterceptorProvider;
    }

    /**
     * 请求前鉴权：返回非 null 即短路返回响应，返回 null 放行继续执行脚本
     */
    @Override
    public Object preHandle(RequestEntity entity) {
        // 鉴权总开关：关闭后所有 magic 接口匿名放行（仅本地调试用）
        if (!properties.isEnabled()) {
            return null;
        }
        // CORS 预检请求不携带 token，直接放行（否则跨域预检会被拦截器挡下）
        if ("OPTIONS".equalsIgnoreCase(entity.getRequest().getMethod())) {
            return null;
        }
        // 白名单放行：匹配完整请求路径（如 /public/**），与 magic-api.prefix 等前缀配置无关
        String path = entity.getRequest().getRequestURI();
        if (properties.getAnonymousPaths().stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path))) {
            return null;
        }
        // 编辑器「测试」请求携带 Magic-Token（编辑器会话），不是页面端的 satoken；
        // 统一交给 AuthorizationInterceptor 校验，兼容 builtin / pkuse-token 两种编辑器鉴权模式
        if (entity.isRequestedFromTest()) {
            Object userId = validateEditorToken(entity.getRequest().getHeader(MAGIC_TOKEN_HEADER));
            if (userId == null) {
                return new JsonBean<>(401, "未登录：请先登录编辑器（右上角）后再测试接口");
            }
            return null;
        }
        // 普通请求：pkuse-token 鉴权（本应用会话存于自己的 Redis db7）
        if (!StpUtil.isLogin()) {
            return new JsonBean<>(401, "未登录或会话已失效，请先完成统一登录或获取开放接口令牌");
        }
        fillScriptContext(entity, StpUtil.getLoginId(), StpUtil.getTokenValue());
        return null;
    }

    /** 校验编辑器会话令牌（Magic-Token 头），有效时返回编辑器用户 */
    private Object validateEditorToken(String magicToken) {
        if (magicToken == null || magicToken.isBlank()) {
            return null;
        }
        AuthorizationInterceptor authorizationInterceptor = authorizationInterceptorProvider.getIfAvailable();
        if (authorizationInterceptor == null) {
            return null;
        }
        try {
            return authorizationInterceptor.getUserByToken(magicToken);
        } catch (Exception e) {
            // 登录已失效（MagicLoginException 等）：视为未登录
            return null;
        }
    }

    /** 把当前登录身份注入脚本上下文：magic 脚本里直接引用变量 loginId / token */
    private void fillScriptContext(RequestEntity entity, Object loginId, String token) {
        MagicScriptContext context = entity.getMagicScriptContext();
        if (context != null) {
            context.set("loginId", loginId);
            context.set("token", token);
        }
    }
}
