package com.bdsoft.authcenter.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 授权中心客户端配置（前缀 auth-center.client）
 *
 * <pre>
 *   auth-center:
 *     client:
 *       base-url: http://localhost:9010     # 授权中心地址
 *       client: sso-client3                 # 本应用标识（与用户中心应用管理登记一致）
 *       secret-key: xxx                     # 接口密钥（与用户中心应用管理登记一致）
 *       cache-ttl-seconds: 60               # 权限缓存有效期
 *       max-stale-seconds: 1800             # 授权中心故障时的陈旧数据兜底窗口
 *       expose-endpoints: true              # 暴露 GET {path-prefix}/menus、{path-prefix}/perms 给前端
 *       path-prefix: /authz                 # 端点路径前缀
 * </pre>
 */
@ConfigurationProperties(prefix = "auth-center.client")
public class AuthCenterClientProperties {

    /** 启用开关（默认 true；配置了 base-url + client 即建议启用） */
    private boolean enabled = true;

    /** 授权中心后端地址 */
    private String baseUrl = "http://localhost:9010";

    /** 本应用标识（与用户中心「应用管理」中的登记一致） */
    private String client;

    /** 接口密钥（与用户中心「应用管理」中的登记一致） */
    private String secretKey;

    /** 权限缓存有效期（秒，默认 60） */
    private long cacheTtlSeconds = 60L;

    /** 授权中心故障时的陈旧数据兜底窗口（秒，默认 30 分钟；超窗 fail-closed 返回空并告警） */
    private long maxStaleSeconds = 1800L;

    /** 是否暴露菜单/权限码端点给前端（默认 false） */
    private boolean exposeEndpoints = false;

    /** 端点路径前缀（默认 /authz） */
    private String pathPrefix = "/authz";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public long getMaxStaleSeconds() {
        return maxStaleSeconds;
    }

    public void setMaxStaleSeconds(long maxStaleSeconds) {
        this.maxStaleSeconds = maxStaleSeconds;
    }

    public boolean isExposeEndpoints() {
        return exposeEndpoints;
    }

    public void setExposeEndpoints(boolean exposeEndpoints) {
        this.exposeEndpoints = exposeEndpoints;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }
}
