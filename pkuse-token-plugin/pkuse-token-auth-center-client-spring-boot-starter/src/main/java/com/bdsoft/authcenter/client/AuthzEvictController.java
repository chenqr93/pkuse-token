package com.bdsoft.authcenter.client;

import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 授权变更失效端点（由自动装配注册，无需 expose-endpoints 开关）
 *
 * <p> 授权中心在菜单/角色/用户授权变更后推送缓存失效通知到这里（签名校验： </p>
 * <p> client + timestamp + nonce 参与签名，密钥为应用自身 secret-key）。 </p>
 * <p> 响应为统一信封 {"code":..,"msg":..,"data":null}；HTTP 状态码保持 200/403 便于推送方判断。 </p>
 *
 * <pre>
 *   POST {path-prefix}/evict?client=..&timestamp=..&nonce=..&sign=..        → 清空全部权限缓存
 *   POST {path-prefix}/evict?client=..&loginId=..&timestamp=..&nonce=..&sign=.. → 仅清除某用户
 *   （兼容 GET，便于授权中心推送实现）
 * </pre>
 */
@RestController
public class AuthzEvictController {

    /** 时间戳允许的最大偏差（15 分钟） */
    private static final long TIMESTAMP_DISPARITY = 1000L * 60 * 15;

    private final AuthzClientService authzClientService;
    private final AuthCenterClientProperties properties;

    /** nonce 一次性缓存（防重放；单机内存实现，集群部署建议改为 Redis） */
    private final Map<String, Long> nonceCache = new ConcurrentHashMap<>();

    public AuthzEvictController(AuthzClientService authzClientService,
                                AuthCenterClientProperties properties) {
        this.authzClientService = authzClientService;
        this.properties = properties;
    }

    @PostMapping("${auth-center.client.path-prefix:/authz}/evict")
    public ResponseEntity<PkuseResult> evictPost(String client, String loginId,
                                                 String timestamp, String nonce, String sign) {
        return evict(client, loginId, timestamp, nonce, sign);
    }

    @GetMapping("${auth-center.client.path-prefix:/authz}/evict")
    public ResponseEntity<PkuseResult> evict(String client, String loginId,
                                             String timestamp, String nonce, String sign) {
        boolean ok = verify(client, loginId, timestamp, nonce, sign);
        if (!ok) {
            return ResponseEntity.status(403)
                    .body(PkuseResult.get(PkuseResult.CODE_NOT_PERMISSION, "签名校验失败", null));
        }
        if (loginId != null && !loginId.isBlank()) {
            authzClientService.evict(loginId);
        } else {
            authzClientService.evictAll();
        }
        return ResponseEntity.ok(PkuseResult.ok("缓存已失效"));
    }

    /** 签名 + 时间窗 + nonce 一次性校验（签名参数按字典序：client < loginId < nonce < timestamp） */
    private boolean verify(String client, String loginId, String timestamp, String nonce, String sign) {
        if (isBlank(client) || isBlank(timestamp) || isBlank(nonce) || isBlank(sign)
                || isBlank(properties.getSecretKey())) {
            return false;
        }
        if (!client.equals(properties.getClient())) {
            return false;
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Math.abs(System.currentTimeMillis() - ts) > TIMESTAMP_DISPARITY) {
            return false;
        }
        StringBuilder source = new StringBuilder("client=").append(client);
        if (loginId != null && !loginId.isBlank()) {
            source.append("&loginId=").append(loginId);
        }
        source.append("&nonce=").append(nonce).append("&timestamp=").append(timestamp);
        String expected = AuthCenterSignUtil.md5(source + "&key=" + properties.getSecretKey());
        if (!expected.equals(sign)) {
            return false;
        }
        long now = System.currentTimeMillis();
        nonceCache.values().removeIf(time -> now - time > TIMESTAMP_DISPARITY);
        return nonceCache.putIfAbsent(nonce, now) == null;
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
