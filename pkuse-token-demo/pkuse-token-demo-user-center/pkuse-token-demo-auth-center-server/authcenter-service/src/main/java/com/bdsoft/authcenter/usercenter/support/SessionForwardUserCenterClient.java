package com.bdsoft.authcenter.usercenter.support;

import com.bdsoft.authcenter.authz.dto.UserQueryDTO;
import com.bdsoft.authcenter.common.config.AuthCenterProperties;
import com.bdsoft.authcenter.usercenter.bo.RegistryUser;
import com.bdsoft.authcenter.usercenter.bo.UserInfo;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户中心数据通道 · simple 模式实现：管理员会话透传
 *
 * <p> 前提：授权中心与用户中心共享会话 Redis（模式一），当前管理员的 token 在用户中心同样有效； </p>
 * <p> 透传 satoken 请求头直接复用用户中心现有接口（/api/auth/me、/api/user/page、/api/user/{id}/clients）， </p>
 * <p> 用户中心零改造，权限随操作管理员（admin 鉴权在用户中心侧完成）。 </p>
 */
@Component
@ConditionalOnProperty(name = "auth-center.sso.integration-mode", havingValue = "simple")
public class SessionForwardUserCenterClient implements UserCenterClient {

    private final AuthCenterProperties properties;
    private final UserCenterHttp http;

    public SessionForwardUserCenterClient(AuthCenterProperties properties, UserCenterHttp http) {
        this.properties = properties;
        this.http = http;
    }

    @Override
    public UserInfo fetchMe() {
        JsonNode data = http.unwrap(http.get(url("/api/auth/me"), tokenHeader()), "获取当前用户");
        UserInfo info = new UserInfo();
        info.setId(data.path("id").asText(null));
        info.setUsername(data.path("username").asText(null));
        info.setNickname(data.path("nickname").asText(null));
        return info;
    }

    @Override
    public RegistryUser.Page getUserPage(UserQueryDTO query) {
        StringBuilder url = new StringBuilder(url("/api/user/page"));
        url.append("?current=").append(query.currentOrDefault())
                .append("&size=").append(query.sizeOrDefault());
        if (query.getUsername() != null) {
            url.append("&username=").append(UserCenterHttp.encode(query.getUsername()));
        }
        if (query.getNickname() != null) {
            url.append("&nickname=").append(UserCenterHttp.encode(query.getNickname()));
        }
        if (query.getStatus() != null) {
            url.append("&status=").append(query.getStatus());
        }
        return parsePage(http.get(url.toString(), tokenHeader()));
    }

    @Override
    public List<String> getUserClients(String userId) {
        JsonNode body = http.get(url("/api/user/" + userId + "/clients"), tokenHeader());
        // 业务码校验（用户中心业务失败为 HTTP 200 + code!=200，如「用户不存在」——孤儿检测依赖此语义）
        int code = body.path("code").asInt(500);
        if (code != 200) {
            throw new com.bdsoft.authcenter.common.exception.BizException(
                    body.path("msg").asText("用户查询失败"));
        }
        // 用户中心该接口直接返回应用标识数组（data 为数组）
        JsonNode data = body.path("data");
        List<String> clients = new ArrayList<>();
        if (data.isArray()) {
            data.forEach(node -> clients.add(node.asText()));
        }
        return clients;
    }

    /** 透传当前会话 token（用户中心 token-name 为 satoken，与本应用 simple 模式共享 Cookie 一致） */
    private Map<String, String> tokenHeader() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("satoken", StpUtil.getTokenValue());
        return headers;
    }

    private String url(String path) {
        return properties.getUserCenter().getBaseUrl() + path;
    }

    /** 解析用户中心分页响应（统一信封 {code, msg, data:{records, total, current, size}}） */
    static RegistryUser.Page parsePage(JsonNode body) {
        RegistryUser.Page page = new RegistryUser.Page();
        JsonNode data = body.path("data");
        page.setTotal(data.path("total").asLong(0));
        page.setCurrent(data.path("current").asLong(1));
        page.setSize(data.path("size").asLong(10));
        List<RegistryUser> records = new ArrayList<>();
        JsonNode recordsNode = data.path("records");
        if (recordsNode.isArray()) {
            for (JsonNode node : recordsNode) {
                RegistryUser user = new RegistryUser();
                user.setId(node.path("id").asText(null));
                user.setUsername(node.path("username").asText(null));
                user.setNickname(node.path("nickname").asText(null));
                user.setRole(node.path("role").asText(null));
                user.setStatus(node.path("status").asInt(0));
                records.add(user);
            }
        }
        page.setRecords(records);
        return page;
    }
}
