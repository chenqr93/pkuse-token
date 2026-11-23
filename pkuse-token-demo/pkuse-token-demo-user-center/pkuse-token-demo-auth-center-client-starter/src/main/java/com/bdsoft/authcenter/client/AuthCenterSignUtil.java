package com.bdsoft.authcenter.client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 授权中心开放接口签名工具（与授权中心 /open/authz/* 验签规则一致）
 *
 * <p> 签名算法：参数（client、loginId、timestamp、nonce）按 key 字典序升序拼接为 k=v&amp;k=v， </p>
 * <p> 末尾追加 &amp;key={secret} 取 MD5（小写）；timestamp 毫秒时间戳（15 分钟窗口），nonce 一次性随机串。 </p>
 */
public final class AuthCenterSignUtil {

    private AuthCenterSignUtil() {
    }

    /** 构造带签名的 GET 地址（loginId 参与签名，防篡改） */
    public static String buildSignedUrl(String baseUrl, Map<String, String> bizParams,
                                        String client, String loginId, String secret) {
        TreeMap<String, String> signParams = new TreeMap<>();
        signParams.put("client", client);
        signParams.put("loginId", loginId);
        signParams.put("nonce", UUID.randomUUID().toString().replace("-", ""));
        signParams.put("timestamp", String.valueOf(System.currentTimeMillis()));

        StringBuilder signSource = new StringBuilder();
        for (Map.Entry<String, String> entry : signParams.entrySet()) {
            if (signSource.length() > 0) {
                signSource.append("&");
            }
            signSource.append(entry.getKey()).append("=").append(entry.getValue());
        }
        String sign = md5(signSource + "&key=" + secret);

        StringBuilder query = new StringBuilder();
        appendParam(query, "client", client);
        appendParam(query, "loginId", loginId);
        appendParam(query, "timestamp", signParams.get("timestamp"));
        appendParam(query, "nonce", signParams.get("nonce"));
        appendParam(query, "sign", sign);
        if (bizParams != null) {
            for (Map.Entry<String, String> entry : bizParams.entrySet()) {
                if (entry.getValue() != null) {
                    appendParam(query, entry.getKey(), entry.getValue());
                }
            }
        }
        return baseUrl + "?" + query;
    }

    private static void appendParam(StringBuilder query, String key, String value) {
        if (query.length() > 0) {
            query.append("&");
        }
        query.append(key).append("=").append(java.net.URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    /** MD5 摘要（小写十六进制） */
    public static String md5(String text) {
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
