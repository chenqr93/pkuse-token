package com.bdsoft.client.sso.support;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * nonce 防重放缓存（内存实现，用于校验认证中心推送消息）
 */
@Component
public class NonceCache {

    private static final long EXPIRE_MILLIS = 1000L * 60 * 15;

    private final Map<String, Long> cache = new ConcurrentHashMap<>();

    /** 若 nonce 未使用过则登记并返回 true；已使用过（重放）返回 false */
    public boolean setIfAbsent(String nonce) {
        clearExpired();
        return cache.putIfAbsent(nonce, System.currentTimeMillis()) == null;
    }

    private void clearExpired() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> now - entry.getValue() > EXPIRE_MILLIS);
    }

}
