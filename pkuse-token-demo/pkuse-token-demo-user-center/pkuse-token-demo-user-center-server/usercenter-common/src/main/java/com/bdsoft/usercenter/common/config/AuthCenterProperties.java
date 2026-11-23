package com.bdsoft.usercenter.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 授权中心（auth-center）对接配置（对应 application.yml 中的 user-center.auth-center.*）
 *
 * <p> SSO userinfo 消息返回应用角色时，认证中心以 user-center 服务身份经签名开放接口 </p>
 * <p> /open/authz/roles 调用授权中心，获取「用户在该应用下的启用角色编码」；本类定义其地址与调用参数。 </p>
 * <p> 授权中心不可用时不阻断 userinfo（角色降级为空数组，见 AuthzRolesClient）。 </p>
 */
@Component
@ConfigurationProperties(prefix = "user-center.auth-center")
public class AuthCenterProperties {

    /** 授权中心基础地址 */
    private String baseUrl = "http://localhost:9010";

    /** 单次调用超时（毫秒） */
    private int timeoutMilliseconds = 5000;

    /** 角色本地缓存时长（秒；0 为不缓存） */
    private int cacheTtlSeconds = 60;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeoutMilliseconds() {
        return timeoutMilliseconds;
    }

    public void setTimeoutMilliseconds(int timeoutMilliseconds) {
        this.timeoutMilliseconds = timeoutMilliseconds;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }
}
