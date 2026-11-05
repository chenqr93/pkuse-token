package com.bdsoft.usercenter.open.support;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务间开放接口签名校验器（/open/registry/**）
 *
 * <p> 供授权中心等系统级调用方访问用户中心的开放数据（应用注册表、用户查询）。 </p>
 * <p> 调用方凭证复用「应用管理」（uc_client）中的登记：client + secret_key。 </p>
 *
 * <p> <b>签名算法</b>（与 Pkuse-Token 参数签名、client5 开放接口一致）： </p>
 * <pre>
 *   ① 参数（client、timestamp、nonce）按 key 字典序升序拼接为 k=v&amp;k=v；
 *   ② 末尾追加 &amp;key={secret}，整体取 MD5（小写）作为 sign；
 *   ③ timestamp 为毫秒时间戳（15 分钟有效窗口），nonce 为一次性随机串（防重放）。
 * </pre>
 */
@Component
public class OpenSignVerifier {

    /** 时间戳允许的最大偏差（15 分钟，与 PkuseSignConfig.timestampDisparity 默认值一致） */
    private static final long TIMESTAMP_DISPARITY = 1000L * 60 * 15;

    /** nonce 一次性缓存（防重放；单机内存实现，集群部署时建议改为 Redis） */
    private final Map<String, Long> nonceCache = new ConcurrentHashMap<>();

    /**
     * 校验签名（timestamp 窗口 + sign 比对 + nonce 一次性）
     *
     * @param client    调用方标识（参与签名）
     * @param timestamp 毫秒时间戳
     * @param nonce     一次性随机串
     * @param sign      参数签名（MD5）
     * @param secret    调用方密钥（uc_client.secret_key）
     */
    public boolean verify(String client, String timestamp, String nonce, String sign, String secret) {
        if (isBlank(client) || isBlank(timestamp) || isBlank(nonce) || isBlank(sign) || isBlank(secret)) {
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
        // sign = md5(client=..&nonce=..&timestamp=..&key=secret)，参数按字典序拼接（client < nonce < timestamp）
        TreeMap<String, String> sorted = new TreeMap<>();
        sorted.put("client", client);
        sorted.put("nonce", nonce);
        sorted.put("timestamp", timestamp);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        sb.append("&key=").append(secret);
        if (!sign.equals(md5(sb.toString()))) {
            return false;
        }
        // nonce 一次性使用，防止重放（顺带清理超出时间窗的历史 nonce）
        long now = System.currentTimeMillis();
        nonceCache.values().removeIf(time -> now - time > TIMESTAMP_DISPARITY);
        return nonceCache.putIfAbsent(nonce, now) == null;
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
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
}
