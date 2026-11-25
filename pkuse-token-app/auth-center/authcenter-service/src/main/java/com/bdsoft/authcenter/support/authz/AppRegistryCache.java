package com.bdsoft.authcenter.support.authz;

import com.bdsoft.authcenter.bo.authz.RegistryApp;
import com.bdsoft.authcenter.support.usercenter.RegistryOpenClient;
import com.bdsoft.pkusetoken.PkuseManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用注册表缓存（两级：内存 + Redis 快照）
 *
 * <p> 应用档案不落地（设计决策）：应用清单与密钥以用户中心 /open/registry/clients 为唯一事实源， </p>
 * <p> 本组件以「实时拉取 + 缓存」方式提供访问：内存 Map 读路径零开销；Redis 快照用于重启恢复 </p>
 * <p> 与用户中心宕机兜底（可随时重建的缓存数据，非业务落地）。 </p>
 *
 * <p> 刷新时机：启动时、定时（auth-center.registry.refresh-cron，默认每 5 分钟）、控制台手动、 </p>
 * <p> 分发接口验签失败时按需刷新重试（消除密钥轮换的定时窗口）。 </p>
 */
@Component
public class AppRegistryCache {

    private static final Logger log = LoggerFactory.getLogger(AppRegistryCache.class);

    /** Redis 快照键（JSON 全量，每次成功拉取覆盖写） */
    public static final String SNAPSHOT_KEY = "authcenter:registry:snapshot";

    private final RegistryOpenClient registryClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 内存缓存（整体替换） */
    private volatile Map<String, RegistryApp> apps = Collections.emptyMap();

    /** 最近刷新时间 */
    private volatile LocalDateTime lastRefreshTime;

    public AppRegistryCache(RegistryOpenClient registryClient) {
        this.registryClient = registryClient;
    }

    /** 启动初始化：拉取失败时从 Redis 快照恢复（快照也没有则保持空并告警） */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            refresh();
        } catch (Exception e) {
            log.warn("应用注册表启动拉取失败，尝试 Redis 快照兜底：{}", e.getMessage());
            loadSnapshot();
        }
    }

    /** 定时刷新（失败保留旧缓存） */
    @Scheduled(cron = "${auth-center.registry.refresh-cron:0 */5 * * * ?}")
    public void scheduledRefresh() {
        refreshQuietly();
    }

    /** 刷新缓存：拉取注册表 → 整体替换内存 → 覆盖写 Redis 快照（失败抛 BizException 由调用方处理） */
    public synchronized void refresh() {
        List<RegistryApp> list = registryClient.fetchRegistry();
        Map<String, RegistryApp> newApps = new LinkedHashMap<>();
        for (RegistryApp app : list) {
            newApps.put(app.getClientId(), app);
        }
        this.apps = Collections.unmodifiableMap(newApps);
        this.lastRefreshTime = LocalDateTime.now();
        try {
            PkuseManager.getSaTokenDao().set(SNAPSHOT_KEY,
                    objectMapper.writeValueAsString(new ArrayList<>(list)), PkuseManager.getSaTokenDao().NEVER_EXPIRE);
        } catch (Exception e) {
            log.warn("应用注册表快照写入失败（不影响内存缓存）：{}", e.getMessage());
        }
        log.info("应用注册表已刷新：{} 个应用，最近刷新时间 {}", newApps.size(), lastRefreshTime);
    }

    /** 静默刷新（失败仅告警，保留旧缓存） */
    public void refreshQuietly() {
        try {
            refresh();
        } catch (Exception e) {
            log.warn("应用注册表定时刷新失败（保留旧缓存）：{}", e.getMessage());
        }
    }

    /** 从 Redis 快照恢复内存缓存（启动拉取失败时兜底） */
    private void loadSnapshot() {
        try {
            String json = PkuseManager.getSaTokenDao().get(SNAPSHOT_KEY);
            if (json == null || json.isBlank()) {
                log.warn("应用注册表快照为空：分发接口验签与控制台应用列表将不可用，请确认用户中心可用后手动刷新");
                return;
            }
            List<RegistryApp> list = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, RegistryApp.class));
            Map<String, RegistryApp> newApps = new LinkedHashMap<>();
            for (RegistryApp app : list) {
                newApps.put(app.getClientId(), app);
            }
            this.apps = Collections.unmodifiableMap(newApps);
            log.warn("应用注册表已从 Redis 快照恢复：{} 个应用（数据可能过期，用户中心恢复后将自动刷新）", newApps.size());
        } catch (Exception e) {
            log.warn("应用注册表快照恢复失败：{}", e.getMessage());
        }
    }

    /** 查询应用（不存在返回 null） */
    public RegistryApp get(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return null;
        }
        return apps.get(clientId);
    }

    /** 全部应用（含禁用） */
    public List<RegistryApp> list() {
        return new ArrayList<>(apps.values());
    }

    /** 应用是否已在注册表且启用 */
    public boolean isEnabled(String clientId) {
        RegistryApp app = get(clientId);
        return app != null && Integer.valueOf(RegistryApp.STATUS_ENABLED).equals(app.getStatus());
    }

    /** 启用应用数 */
    public long countEnabled() {
        return apps.values().stream()
                .filter(app -> Integer.valueOf(RegistryApp.STATUS_ENABLED).equals(app.getStatus()))
                .count();
    }

    /** 最近刷新时间（可能为 null：从未成功拉取且无快照） */
    public LocalDateTime lastRefreshTime() {
        return lastRefreshTime;
    }
}
