package com.bdsoft.authcenter.support.usercenter;

import com.bdsoft.authcenter.dto.authz.UserQueryDTO;
import com.bdsoft.authcenter.common.config.AuthCenterProperties;
import com.bdsoft.authcenter.bo.usercenter.RegistryUser;
import com.bdsoft.authcenter.bo.usercenter.UserInfo;
import com.bdsoft.pkusetoken.sso.message.PkuseSsoMessage;
import com.bdsoft.pkusetoken.sso.template.PkuseSsoClientUtil;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoClientConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户中心数据通道 · ticket 模式实现：签名调开放接口
 *
 * <p> ticket 模式下授权中心持有独立会话，拿不到用户中心有效的 satoken（ticket 交换不回传 token）， </p>
 * <p> 用户查询改走用户中心新增的签名开放接口 /open/registry/users*； </p>
 * <p> 当前用户信息走 SSO userinfo 消息（SDK 签名通道，返回纯身份信息 id/username/nickname）。 </p>
 */
@Component
@ConditionalOnProperty(name = "auth-center.sso.integration-mode", havingValue = "ticket", matchIfMissing = true)
public class SignedOpenUserCenterClient implements UserCenterClient {

    private final AuthCenterProperties properties;
    private final UserCenterHttp http;

    public SignedOpenUserCenterClient(AuthCenterProperties properties, UserCenterHttp http) {
        this.properties = properties;
        this.http = http;
    }

    /** 当前用户信息：SSO userinfo 消息（认证中心消息处理器返回纯身份信息，data={id,username,nickname}） */
    @Override
    public UserInfo fetchMe() {
        if (!StpUtil.isLogin()) {
            throw new com.bdsoft.authcenter.common.exception.BizException("尚未登录，无法获取用户信息");
        }
        PkuseSsoMessage message = new PkuseSsoMessage();
        message.setType("userinfo");
        message.set("loginId", StpUtil.getLoginId());
        Object result = PkuseSsoClientUtil.pushMessageAsSaResult(message);
        if (!(result instanceof PkuseResult)) {
            throw new com.bdsoft.authcenter.common.exception.BizException("用户信息消息通道响应异常");
        }
        PkuseResult pkuseResult = (PkuseResult) result;
        if (pkuseResult.getCode() != 200) {
            throw new com.bdsoft.authcenter.common.exception.BizException(
                    "获取用户信息失败：" + pkuseResult.getMsg());
        }
        // userinfo 处理器统一信封返回，身份字段在 data 内
        if (!(pkuseResult.getData() instanceof Map<?, ?> data)) {
            throw new com.bdsoft.authcenter.common.exception.BizException("用户信息消息通道响应格式异常");
        }
        UserInfo info = new UserInfo();
        info.setId(data.get("id") == null ? null : String.valueOf(data.get("id")));
        info.setUsername(data.get("username") == null ? null : String.valueOf(data.get("username")));
        info.setNickname(data.get("nickname") == null ? null : String.valueOf(data.get("nickname")));
        return info;
    }

    @Override
    public RegistryUser.Page getUserPage(UserQueryDTO query) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("current", String.valueOf(query.currentOrDefault()));
        params.put("size", String.valueOf(query.sizeOrDefault()));
        if (query.getUsername() != null) {
            params.put("username", query.getUsername());
        }
        if (query.getNickname() != null) {
            params.put("nickname", query.getNickname());
        }
        if (query.getStatus() != null) {
            params.put("status", String.valueOf(query.getStatus()));
        }
        JsonNode body = http.get(signedUrl("/open/registry/users", params), null);
        return SessionForwardUserCenterClient.parsePage(body);
    }

    @Override
    public List<String> getUserClients(String userId) {
        Map<String, String> params = new LinkedHashMap<>();
        JsonNode body = http.get(signedUrl("/open/registry/users/" + userId + "/clients", params), null);
        // 业务码校验（HTTP 200 但 code!=500 之外的业务失败需转为异常，如「用户不存在」——孤儿检测依赖此语义）
        int code = body.path("code").asInt(500);
        if (code != 200) {
            throw new com.bdsoft.authcenter.common.exception.BizException(
                    body.path("msg").asText("用户查询失败"));
        }
        List<String> clients = new ArrayList<>();
        JsonNode data = body.path("data");
        if (data.isArray()) {
            data.forEach(node -> clients.add(node.asText()));
        }
        return clients;
    }

    /** 构造签名地址（client/timestamp/nonce 参与签名，业务参数不参与——与用户中心验签规则一致） */
    private String signedUrl(String path, Map<String, String> params) {
        PkuseSsoClientConfig cfg = PkuseSsoManager.getClientConfig();
        return OpenSignUtil.buildSignedUrl(properties.getUserCenter().getBaseUrl() + path,
                params, cfg.getClient(), cfg.getSecretKey());
    }
}
