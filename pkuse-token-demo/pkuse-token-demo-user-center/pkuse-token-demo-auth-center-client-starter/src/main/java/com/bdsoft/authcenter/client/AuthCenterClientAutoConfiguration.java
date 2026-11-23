package com.bdsoft.authcenter.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 授权中心客户端自动装配
 *
 * <p> 引入本依赖并配置 auth-center.client.* 即启用；各应用「引依赖 + 写配置」完成权限层接入， </p>
 * <p> 登录仍走用户中心 SSO（两层解耦，本 starter 不做登录/会话/角色管理）。 </p>
 *
 * <p> pkuse-token 为 optional 依赖：相关 Bean 放在 @ConditionalOnClass 保护的嵌套配置类中， </p>
 * <p> NoSdk 应用（不含 pkuse-token）引入本 starter 也不会类加载失败（仅无法使用 StpInterface 适配与菜单端点）。 </p>
 */
@AutoConfiguration
@EnableConfigurationProperties(AuthCenterClientProperties.class)
@ConditionalOnProperty(prefix = "auth-center.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthCenterClientAutoConfiguration {

    /** 权限客户端（签名调用 + 缓存 + 陈旧兜底）：无可选依赖，任何应用可用 */
    @Bean
    @ConditionalOnMissingBean
    public AuthzClientService authzClientService(AuthCenterClientProperties properties) {
        return new AuthzClientService(properties);
    }

    /** pkuse-token 相关能力（classpath 存在 pkuse-token 时装配） */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.bdsoft.pkusetoken.stp.StpInterface")
    public static class PkuseTokenIntegrationConfiguration {

        /**
         * StpInterface 适配：getPermissionList 返回授权中心分发的权限码，
         * @PkuseCheckPermission / StpUtil.checkPermission 开箱即用；
         * 应用已有自定义 StpInterface 时自动退避（角色维度留给应用自身实现）。
         */
        @Bean("authCenterStpInterface")
        @ConditionalOnMissingBean(com.bdsoft.pkusetoken.stp.StpInterface.class)
        public PkuseStpInterfaceAdapter authCenterStpInterface(AuthzClientService authzClientService) {
            return new PkuseStpInterfaceAdapter(authzClientService);
        }

        /**
         * 菜单/权限码端点（expose-endpoints=true 时暴露给前端）：
         * GET {path-prefix}/menus、GET {path-prefix}/perms（当前登录用户）。
         */
        @Bean
        @ConditionalOnProperty(prefix = "auth-center.client", name = "expose-endpoints", havingValue = "true")
        public AuthzClientController authzClientController(AuthzClientService authzClientService,
                                                           AuthCenterClientProperties properties) {
            return new AuthzClientController(authzClientService, properties);
        }

        /**
         * 授权变更失效端点（始终注册，无需 expose-endpoints 开关）：
         * 接收授权中心推送的缓存失效通知（POST/GET {path-prefix}/evict，签名校验）。
         */
        @Bean
        public AuthzEvictController authzEvictController(AuthzClientService authzClientService,
                                                         AuthCenterClientProperties properties) {
            return new AuthzEvictController(authzClientService, properties);
        }
    }
}
