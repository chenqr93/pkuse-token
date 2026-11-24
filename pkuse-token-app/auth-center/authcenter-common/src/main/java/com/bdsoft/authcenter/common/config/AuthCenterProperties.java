package com.bdsoft.authcenter.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 授权中心平台配置（前缀 auth-center）
 *
 * <pre>
 *   auth-center:
 *     sso:
 *       integration-mode: ticket      # simple=模式一式直通（同域+同Redis） / ticket=模式二/三式（默认，任意拓扑）
 *     registry:
 *       refresh-cron: 0 *&#47;5 * * * ?   # 应用注册表定时刷新周期
 *     user-center:
 *       base-url: http://localhost:9000  # 用户中心地址
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "auth-center")
public class AuthCenterProperties {

    /** SSO 对接配置 */
    private Sso sso = new Sso();

    /** 应用注册表配置 */
    private Registry registry = new Registry();

    /** 用户中心对接配置 */
    private UserCenter userCenter = new UserCenter();

    /** 开发免登录配置（仅 dev profile 配置；生产不配置即自动关闭） */
    private DevAuth devAuth = new DevAuth();

    
    public static class Sso {

        /** 对接模式：simple（模式一式直通）/ ticket（模式二/三式，默认，任意拓扑通用） */
        public static final String MODE_SIMPLE = "simple";

        /** 对接模式：ticket */
        public static final String MODE_TICKET = "ticket";

        /** 对接模式（默认 ticket） */
        private String integrationMode = MODE_TICKET;

        public String getIntegrationMode() {
            return integrationMode;
        }

        public void setIntegrationMode(String integrationMode) {
            this.integrationMode = integrationMode;
        }

        /** 当前是否 ticket 模式 */
        public boolean isTicketMode() {
            return MODE_TICKET.equalsIgnoreCase(integrationMode);
        }
    }

    
    public static class Registry {

        /** 注册表定时刷新 cron（默认每 5 分钟） */
        private String refreshCron = "0 */5 * * * ?";

        public String getRefreshCron() {
            return refreshCron;
        }

        public void setRefreshCron(String refreshCron) {
            this.refreshCron = refreshCron;
        }
    }

    
    public static class UserCenter {

        /** 用户中心后端地址 */
        private String baseUrl = "http://localhost:9000";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class DevAuth {

        /** 是否启用开发免登录（默认关闭） */
        private boolean enabled = false;

        /** 注入的登录 ID（内置管理员为 1） */
        private String loginId = "1";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLoginId() {
            return loginId;
        }

        public void setLoginId(String loginId) {
            this.loginId = loginId;
        }
    }

    public DevAuth getDevAuth() {
        return devAuth;
    }

    public void setDevAuth(DevAuth devAuth) {
        this.devAuth = devAuth;
    }

    public Sso getSso() {
        return sso;
    }

    public void setSso(Sso sso) {
        this.sso = sso;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public UserCenter getUserCenter() {
        return userCenter;
    }

    public void setUserCenter(UserCenter userCenter) {
        this.userCenter = userCenter;
    }
}
