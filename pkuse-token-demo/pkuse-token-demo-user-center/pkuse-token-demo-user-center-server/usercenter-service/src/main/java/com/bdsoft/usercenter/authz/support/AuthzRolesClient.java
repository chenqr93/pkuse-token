package com.bdsoft.usercenter.authz.support;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.usercenter.client.po.UcClient;
import com.bdsoft.usercenter.client.service.UcClientService;
import com.bdsoft.usercenter.common.config.AuthCenterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 授权中心应用角色客户端（userinfo 消息回填 role 数组使用）
 *
 * <p> /open/authz/roles 沿用开放分发接口语义：client 既是验签身份也是角色归属应用， </p>
 * <p> 故本客户端以「目标应用」自己的 client + secret_key（uc_client 登记，认证中心为密钥事实源） </p>
 * <p> 按四参 MD5 签名规范（client/loginId/nonce/timestamp 字典序 + &amp;key=secret） </p>
 * <p> 调用 GET /open/authz/roles，获取「用户在该应用下的启用角色编码」。 </p>
 *
 * <p> 带 TTL 本地缓存；应用未登记 / 授权中心未部署 / 超时 / 验签失败（含应用被禁用）时 </p>
 * <p> 一律降级为空列表并记录 warn 日志，绝不阻断 userinfo 主流程（此时 role 恒为 []）。 </p>
 */
@Component
public class AuthzRolesClient {

    private static final Logger log = LoggerFactory.getLogger(AuthzRolesClient.class);

    /** 授权中心成功业务码（PkuseResult） */
    private static final int CODE_OK = 200;

    private final AuthCenterProperties properties;
    private final UcClientService clientService;
    private final HttpClient httpClient;

    /** 角色本地缓存：key = client:loginId（单机内存实现，TTL 由配置项决定） */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public AuthzRolesClient(AuthCenterProperties properties, UcClientService clientService) {
        this.properties = properties;
        this.clientService = clientService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * 查询用户在某应用下授权中心分配的启用角色编码列表（带 TTL 缓存；失败降级返回空列表）
     */
    public List<String> getRoles(String clientId, String loginId) {
        if (isBlank(clientId) || isBlank(loginId)) {
            return Collections.emptyList();
        }
        String cacheKey = clientId + ":" + loginId;
        CacheEntry entry = cache.get(cacheKey);
        long now = System.currentTimeMillis();
        if (entry != null && entry.expireAt > now) {
            return entry.roles;
        }
        List<String> roles = fetchRoles(clientId, loginId);
        long ttl = properties.getCacheTtlSeconds();
        if (ttl > 0) {
            cache.put(cacheKey, new CacheEntry(Collections.unmodifiableList(roles), now + ttl * 1000L));
        }
        return roles;
    }

    /** 以目标应用身份签名调用 /open/authz/roles；任何异常（未登记/未部署/超时/验签失败）均降级为空列表 */
    private List<String> fetchRoles(String clientId, String loginId) {
        try {
            String secret = secretOf(clientId);
            if (isBlank(secret)) {
                log.warn("获取应用角色降级：uc_client 中未登记 {} 的 secret_key", clientId);
                return Collections.emptyList();
            }
            // sign = md5(client=..&loginId=..&nonce=..&timestamp=..&key=secret)，参数按字典序（与授权中心验签器一致）
            String timestamp = String.valueOf(System.currentTimeMillis());
            String nonce = UUID.randomUUID().toString().replace("-", "");
            TreeMap<String, String> signParams = new TreeMap<>();
            signParams.put("client", clientId);
            signParams.put("loginId", loginId);
            signParams.put("nonce", nonce);
            signParams.put("timestamp", timestamp);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> param : signParams.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(param.getKey()).append("=").append(param.getValue());
            }
            sb.append("&key=").append(secret);
            String sign = md5(sb.toString());

            String url = properties.getBaseUrl() + "/open/authz/roles"
                    + "?client=" + urlEncode(clientId)
                    + "&loginId=" + urlEncode(loginId)
                    + "&nonce=" + nonce
                    + "&timestamp=" + timestamp
                    + "&sign=" + sign;
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(properties.getTimeoutMilliseconds()))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("获取应用角色降级：授权中心响应 HTTP {}（client={}, loginId={}）", response.statusCode(), clientId, loginId);
                return Collections.emptyList();
            }
            return parseRoles(response.body(), clientId, loginId);
        } catch (Exception e) {
            log.warn("获取应用角色降级：调用授权中心失败（client={}, loginId={}）：{}", clientId, loginId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 解析 PkuseResult：code=200 且 data 为字符串数组时返回，其余降级空列表 */
    private List<String> parseRoles(String body, String clientId, String loginId) {
        try {
            Map<String, Object> result = PkuseManager.getSaJsonTemplate().jsonToMap(body);
            if (!(result.get("code") instanceof Number code) || code.intValue() != CODE_OK) {
                log.warn("获取应用角色降级：授权中心返回 code={}（client={}, loginId={}）", result.get("code"), clientId, loginId);
                return Collections.emptyList();
            }
            Object data = result.get("data");
            if (!(data instanceof List<?> list)) {
                return Collections.emptyList();
            }
            return list.stream().map(String::valueOf).toList();
        } catch (Exception e) {
            log.warn("获取应用角色降级：响应解析失败（client={}, loginId={}）：{}", clientId, loginId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 读取目标应用在 uc_client 中登记的密钥（授权中心注册表缓存的事实源） */
    private String secretOf(String clientId) {
        UcClient client = clientService.getByClientId(clientId);
        return client == null ? null : client.getSecretKey();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String md5(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 摘要失败", e);
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    /** 缓存条目（角色列表 + 过期时间戳） */
    private static final class CacheEntry {

        private final List<String> roles;
        private final long expireAt;

        private CacheEntry(List<String> roles, long expireAt) {
            this.roles = roles;
            this.expireAt = expireAt;
        }
    }
}
