package com.bdsoft.client.sso;

import com.bdsoft.client.config.SsoClientProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * SSO 参数签名工具（手动实现，与 Pkuse-Token 签名算法一致）
 *
 * <p> 签名算法：将参数（排除 sign）按 key 字典序升序拼接为 k=v&amp;k=v， </p>
 * <p> 后追加 &amp;key={secretKey}，整体取 MD5（小写）作为 sign； </p>
 * <p> 同时携带 timestamp（毫秒）与 nonce（随机串）防重放。 </p>
 */
@Component
public class SsoSignUtil {

    /** 时间戳允许的最大偏差（与官方 PkuseSignConfig.timestampDisparity 默认值一致：15 分钟） */
    private static final long TIMESTAMP_DISPARITY = 1000L * 60 * 15;

    private final SsoClientProperties properties;

    public SsoSignUtil(SsoClientProperties properties) {
        this.properties = properties;
    }

    /** 计算签名（不含 sign 字段的参数集） */
    public String computeSign(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        sorted.remove("sign");
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        sb.append("&key=").append(properties.getSecretKey());
        return md5(sb.toString());
    }

    /** 向参数 Map 注入 timestamp、nonce、sign */
    public void addSignParams(Map<String, String> params) {
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        params.put("nonce", getRandomString(20));
        params.put("sign", computeSign(params));
    }

    /**
     * 校验请求中的 timestamp、nonce、sign（用于校验认证中心推送的消息）
     *
     * @param params  含 sign 的请求参数
     * @param nonceCache 用于防重放的 nonce 缓存（key=nonce）
     */
    public boolean verifySign(Map<String, String> params, NonceCache nonceCache) {
        String sign = params.get("sign");
        String timestamp = params.get("timestamp");
        String nonce = params.get("nonce");
        if (isEmpty(sign) || isEmpty(timestamp) || isEmpty(nonce)) {
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
        if (!sign.equals(computeSign(params))) {
            return false;
        }
        // nonce 一次性使用，防止重放
        return nonceCache.setIfAbsent(nonce);
    }

    public static String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 摘要失败", e);
        }
    }

    public static String getRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }

}
