package com.bdsoft.authcenter.support.authz;

import com.bdsoft.authcenter.bo.authz.RegistryApp;
import com.bdsoft.authcenter.support.usercenter.OpenSignUtil;
import com.bdsoft.authcenter.support.usercenter.UserCenterHttp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 授权变更推送器（V2：授权变更主动失效通知）
 *
 * <p> 菜单/角色/用户授权变更后，向「启用 + isPush=1」的应用推送缓存失效通知（authzChanged）： </p>
 * <p> 目标地址由注册表的 pushUrl 推导（origin + /authz/evict，starter 自动暴露的失效端点）， </p>
 * <p> 签名与开放接口一致（client + timestamp + nonce 参与签名，密钥为该应用密钥）。 </p>
 *
 * <p> 异步单线程执行、失败仅告警——推送不可用不影响管理操作；各应用即使收不到通知， </p>
 * <p> 也会在其缓存 TTL（默认 60s）到期后自然拉到最新数据。 </p>
 */
@Component
public class AuthzChangePusher {

    private static final Logger log = LoggerFactory.getLogger(AuthzChangePusher.class);

    /** 失效端点路径（与 starter 的默认 path-prefix 一致） */
    private static final String EVICT_PATH = "/authz/evict";

    @Resource
    private AppRegistryCache registryCache;

    @Resource
    private UserCenterHttp http;

    /** 推送线程（守护线程：不阻塞 JVM 关闭） */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "authz-change-pusher");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 向指定应用推送授权变更通知（clientId 为空时通知全部启用且开启推送的应用）
     *
     * @param reason 变更原因（日志用，如「菜单管理-新增菜单」）
     */
    public void push(String clientId, String reason) {
        List<RegistryApp> targets = registryCache.list().stream()
                .filter(app -> Integer.valueOf(RegistryApp.STATUS_ENABLED).equals(app.getStatus()))
                .filter(app -> Integer.valueOf(1).equals(app.getIsPush()))
                .filter(app -> app.getPushUrl() != null && !app.getPushUrl().isBlank())
                .filter(app -> clientId == null || clientId.isBlank() || clientId.equals(app.getClientId()))
                .toList();
        if (targets.isEmpty()) {
            return;
        }
        executor.execute(() -> {
            for (RegistryApp app : targets) {
                try {
                    http.get(buildEvictSignedUrl(evictUrl(app.getPushUrl()),
                            app.getClientId(), app.getSecretKey()), null);
                    log.info("授权变更通知已推送：app={}（{}）", app.getClientId(), reason);
                } catch (Exception e) {
                    log.warn("授权变更通知推送失败（app 将在缓存 TTL 到期后自然刷新）：app={}，{}，{}",
                            app.getClientId(), e.getMessage(), reason);
                }
            }
        });
    }

    /** 构造失效通知地址（签名参数：client、timestamp、nonce，与 starter 失效端点校验一致） */
    private static String buildEvictSignedUrl(String baseUrl, String client, String secret) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = java.util.UUID.randomUUID().toString().replace("-", "");
        String sign = OpenSignUtil.md5("client=" + client + "&nonce=" + nonce
                + "&timestamp=" + timestamp + "&key=" + secret);
        return baseUrl + "?client=" + UserCenterHttp.encode(client)
                + "&timestamp=" + timestamp + "&nonce=" + nonce + "&sign=" + sign;
    }

    /**
     * 由 pushUrl 推导失效端点：去掉结尾的 SSO 推送路径（/sso/pushC）保留其上下文前缀，
     * 再拼 /authz/evict（starter 自动暴露的失效端点）。
     *
     * <p> 只取 origin 的旧算法在应用带 servlet context-path 部署时（如 /client3）会丢前缀导致 404。 </p>
     */
    private static String evictUrl(String pushUrl) {
        String base = pushUrl;
        int idx = base.indexOf("/sso/pushC");
        if (idx > 0) {
            base = base.substring(0, idx);
        }
        URI uri = URI.create(base);
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return uri.getScheme() + "://" + uri.getRawAuthority() + path + EVICT_PATH;
    }
}
