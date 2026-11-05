package com.bdsoft.authcenter.usercenter.support;

import com.bdsoft.authcenter.authz.bo.RegistryApp;
import com.bdsoft.authcenter.common.config.AuthCenterProperties;
import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoClientConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用注册表签名调用客户端（两种 SSO 模式共用）
 *
 * <p> 调用用户中心 GET /open/registry/clients（client=auth-center + MD5 签名）， </p>
 * <p> 拉取全部未删除应用档案（含密钥），供 AppRegistryCache 维护本地缓存。 </p>
 * <p> 调用方凭证取 sa-token.sso-client 配置（与 SSO 登录共用一份登记）。 </p>
 */
@Component
public class RegistryOpenClient {

    private final AuthCenterProperties properties;
    private final UserCenterHttp http;

    public RegistryOpenClient(AuthCenterProperties properties, UserCenterHttp http) {
        this.properties = properties;
        this.http = http;
    }

    /** 拉取全部应用注册表（未删除，含禁用） */
    public List<RegistryApp> fetchRegistry() {
        PkuseSsoClientConfig cfg = PkuseSsoManager.getClientConfig();
        String url = OpenSignUtil.buildSignedUrl(
                properties.getUserCenter().getBaseUrl() + "/open/registry/clients",
                null, cfg.getClient(), cfg.getSecretKey());
        JsonNode data = http.unwrap(http.get(url, null), "应用注册表拉取");
        List<RegistryApp> list = new ArrayList<>();
        if (data != null && data.isArray()) {
            for (JsonNode node : data) {
                RegistryApp app = new RegistryApp();
                app.setClientId(node.path("clientId").asText(null));
                app.setClientName(node.path("clientName").asText(null));
                app.setEntryUrl(node.path("entryUrl").asText(null));
                app.setSecretKey(node.path("secretKey").asText(null));
                app.setStatus(node.path("status").asInt(RegistryApp.STATUS_DISABLED));
                app.setIsPush(node.path("isPush").asInt(0));
                app.setPushUrl(node.path("pushUrl").asText(null));
                if (app.getClientId() != null && !app.getClientId().isBlank()) {
                    list.add(app);
                }
            }
        }
        return list;
    }
}
