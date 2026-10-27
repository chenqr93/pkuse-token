package com.bdsoft.client.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * NoSdk 客户端配置（对应 application.yml 中的 sso-client.*）
 */
@Component
@ConfigurationProperties(prefix = "sso-client")
public class SsoClientProperties {

    /** 应用标识（需与用户中心「应用管理」中的登记一致） */
    private String clientId;

    /** SSO-Server 端（认证中心）主机地址 */
    private String serverUrl;

    /** API 接口调用秘钥（与认证中心登记一致） */
    private String secretKey;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    /** 认证中心：单点登录授权地址 */
    public String authUrl() {
        return serverUrl + "/sso/auth";
    }

    /** 认证中心：消息推送/校验地址（checkTicket、signout、userinfo 均走此入口） */
    public String pushSUrl() {
        return serverUrl + "/sso/pushS";
    }

}
