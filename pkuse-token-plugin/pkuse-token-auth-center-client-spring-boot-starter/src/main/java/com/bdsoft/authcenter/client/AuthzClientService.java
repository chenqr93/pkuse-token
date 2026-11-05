package com.bdsoft.authcenter.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 授权中心权限数据客户端：签名调用 + 按 loginId 内存缓存 + 陈旧兜底
 *
 * <p> 能力：拉取「某用户在本应用的菜单树 / 权限码集合 / 角色编码集合」 </p>
 * <p>（授权中心 /open/authz/menus、/open/authz/perms、/open/authz/roles）。 </p>
 *
 * <p> 缓存与降级策略： </p>
 * <ul>
 *   <li>TTL 内直接命中（默认 60s，可配 cache-ttl-seconds）；</li>
 *   <li>过期后回源刷新；刷新失败（授权中心不可用）时若缓存陈龄在 max-stale-seconds（默认 30 分钟）内，
 *       沿用陈旧数据（可用性优先，注意：窗口内已回收的权限仍生效）；</li>
 *   <li>超窗 fail-closed：返回空集合并告警（安全优先，表现为无菜单、受控接口 403）。</li>
 * </ul>
 */
public class AuthzClientService {

    private static final Logger log = LoggerFactory.getLogger(AuthzClientService.class);

    /** 缓存条目数上限（超过后整体清空，避免无界增长；demo 规模足够） */
    private static final int MAX_CACHE_SIZE = 10000;

    private final AuthCenterClientProperties properties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** 按 loginId 维度缓存（menus / perms / roles 各自独立刷新时间） */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public AuthzClientService(AuthCenterClientProperties properties) {
        this.properties = properties;
    }

    /** 用户可见菜单树（缓存/降级策略见类注释；fail-closed 返回空列表） */
    public List<Map<String, Object>> getMenus(String loginId) {
        CacheEntry entry = cache.computeIfAbsent(loginId, k -> new CacheEntry());
        if (!needRefresh(entry.menusFetchTime)) {
            return entry.menus;
        }
        List<Map<String, Object>> fresh = fetchMenus(loginId);
        if (fresh != null) {
            entry.menus = fresh;
            entry.menusFetchTime = System.currentTimeMillis();
            return fresh;
        }
        return staleOrClosed(entry.menusFetchTime, "menus", entry.menus, Collections.emptyList());
    }

    /** 用户权限码集合（按钮级鉴权用；fail-closed 返回空列表） */
    public List<String> getPerms(String loginId) {
        CacheEntry entry = cache.computeIfAbsent(loginId, k -> new CacheEntry());
        if (!needRefresh(entry.permsFetchTime)) {
            return entry.perms;
        }
        List<String> fresh = fetchPerms(loginId);
        if (fresh != null) {
            entry.perms = fresh;
            entry.permsFetchTime = System.currentTimeMillis();
            return fresh;
        }
        return staleOrClosed(entry.permsFetchTime, "perms", entry.perms, Collections.emptyList());
    }

    /** 用户在本应用的角色编码集合（角色级鉴权用；fail-closed 返回空列表） */
    public List<String> getRoles(String loginId) {
        CacheEntry entry = cache.computeIfAbsent(loginId, k -> new CacheEntry());
        if (!needRefresh(entry.rolesFetchTime)) {
            return entry.roles;
        }
        List<String> fresh = fetchRoles(loginId);
        if (fresh != null) {
            entry.roles = fresh;
            entry.rolesFetchTime = System.currentTimeMillis();
            return fresh;
        }
        return staleOrClosed(entry.rolesFetchTime, "roles", entry.roles, Collections.emptyList());
    }

    /** 预热缓存（应用可在登录成功后调用，减少首屏一次回源；失败仅告警） */
    public void warmUp(String loginId) {
        try {
            getMenus(loginId);
            getPerms(loginId);
            getRoles(loginId);
        } catch (Exception e) {
            log.warn("权限缓存预热失败（loginId={}）：{}", loginId, e.getMessage());
        }
    }

    /** 清除某用户的缓存（登出时可选调用） */
    public void evict(String loginId) {
        cache.remove(loginId);
    }

    /** 清空全部权限缓存（授权中心推送失效通知时调用） */
    public void evictAll() {
        cache.clear();
        log.info("权限缓存已全部失效（授权中心推送的授权变更通知）");
    }

    // ------------------------- 内部实现 -------------------------

    private boolean needRefresh(long fetchTime) {
        return System.currentTimeMillis() - fetchTime > properties.getCacheTtlSeconds() * 1000;
    }

    /** 回源失败：陈龄在兜底窗口内沿用陈旧数据；超窗 fail-closed 返回空并告警 */
    private <T> List<T> staleOrClosed(long fetchTime, String what, List<T> cached, List<T> empty) {
        long staleMillis = System.currentTimeMillis() - fetchTime;
        if (fetchTime > 0 && staleMillis <= properties.getMaxStaleSeconds() * 1000) {
            log.warn("授权中心不可用，{} 沿用陈旧缓存（陈龄 {} 秒，兜底窗口 {} 秒）",
                    what, staleMillis / 1000, properties.getMaxStaleSeconds());
            return cached;
        }
        log.warn("授权中心不可用且缓存超窗，{} fail-closed 返回空集合（loginId 缓存已失效）", what);
        return empty;
    }

    /** 拉取菜单树（失败返回 null） */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchMenus(String loginId) {
        try {
            JsonNode data = get("/open/authz/menus", loginId);
            if (data == null || !data.isArray()) {
                return new ArrayList<>();
            }
            return objectMapper.convertValue(data, List.class);
        } catch (Exception e) {
            log.warn("菜单树拉取失败（loginId={}）：{}", loginId, e.getMessage());
            return null;
        }
    }

    /** 拉取权限码集合（失败返回 null） */
    private List<String> fetchPerms(String loginId) {
        try {
            JsonNode data = get("/open/authz/perms", loginId);
            if (data == null || !data.isArray()) {
                return new ArrayList<>();
            }
            List<String> perms = new ArrayList<>();
            data.forEach(node -> perms.add(node.asText()));
            return perms;
        } catch (Exception e) {
            log.warn("权限码拉取失败（loginId={}）：{}", loginId, e.getMessage());
            return null;
        }
    }

    /** 拉取角色编码集合（失败返回 null） */
    private List<String> fetchRoles(String loginId) {
        try {
            JsonNode data = get("/open/authz/roles", loginId);
            if (data == null || !data.isArray()) {
                return new ArrayList<>();
            }
            List<String> roles = new ArrayList<>();
            data.forEach(node -> roles.add(node.asText()));
            return roles;
        } catch (Exception e) {
            log.warn("角色编码拉取失败（loginId={}）：{}", loginId, e.getMessage());
            return null;
        }
    }

    /** 签名调用授权中心开放接口并解包 data（code != 200 抛异常） */
    private JsonNode get(String path, String loginId) throws Exception {
        if (cache.size() > MAX_CACHE_SIZE) {
            cache.clear();
            log.warn("权限缓存超过 {} 条，已整体清空", MAX_CACHE_SIZE);
        }
        String url = AuthCenterSignUtil.buildSignedUrl(
                properties.getBaseUrl() + path, null,
                properties.getClient(), loginId, properties.getSecretKey());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode body = objectMapper.readTree(response.body());
        int code = body.path("code").asInt(500);
        if (code != 200) {
            throw new IllegalStateException("code=" + code + "，msg=" + body.path("msg").asText(""));
        }
        return body.get("data");
    }

    /** 缓存条目（三类数据各自独立记时，避免并发拉取时互相把对方判成「未过期」而返回初始空值） */
    private static class CacheEntry {
        volatile List<Map<String, Object>> menus = Collections.emptyList();
        volatile long menusFetchTime;
        volatile List<String> perms = Collections.emptyList();
        volatile long permsFetchTime;
        volatile List<String> roles = Collections.emptyList();
        volatile long rolesFetchTime;
    }
}
