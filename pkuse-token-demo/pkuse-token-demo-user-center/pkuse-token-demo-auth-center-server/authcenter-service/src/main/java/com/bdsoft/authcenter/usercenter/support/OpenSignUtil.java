package com.bdsoft.authcenter.usercenter.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 服务间调用 MD5 签名工具（与用户中心 /open/registry/* 校验规则一致）
 *
 * <p> 签名算法：参与签名的参数按 key 字典序升序拼接为 k=v&amp;k=v，末尾追加 &amp;key={secret}， </p>
 * <p> 整体取 MD5（小写）作为 sign；timestamp 为毫秒时间戳（15 分钟窗口），nonce 为一次性随机串。 </p>
 *
 * <p> 注意：用户中心侧校验固定只签 client、timestamp、nonce 三个参数， </p>
 * <p> 其余查询参数（username 等）不参与签名（服务端验签时忽略）。 </p>
 */
public final class OpenSignUtil {

    private OpenSignUtil() {
    }

    /** 生成一次性 nonce */
    public static String newNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 当前毫秒时间戳 */
    public static String newTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * 构造带签名的 GET 地址
     *
     * @param baseUrl 完整接口地址（不含查询参数）
     * @param params  业务参数（不含 client/timestamp/nonce/sign，值为 null 的跳过）
     * @param client  调用方标识（参与签名）
     * @param secret  调用方密钥
     */
    public static String buildSignedUrl(String baseUrl, Map<String, String> params, String client, String secret) {
        TreeMap<String, String> signParams = new TreeMap<>();
        signParams.put("client", client);
        signParams.put("nonce", newNonce());
        signParams.put("timestamp", newTimestamp());

        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : signParams.entrySet()) {
            appendParam(query, entry.getKey(), entry.getValue());
        }
        String sign = md5(concatSorted(signParams) + "&key=" + secret);
        appendParam(query, "sign", sign);
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getValue() != null) {
                    appendParam(query, entry.getKey(), entry.getValue());
                }
            }
        }
        return baseUrl + "?" + query;
    }

    /** 按字典序拼接 k=v&k=v（用于计算签名） */
    private static String concatSorted(TreeMap<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private static void appendParam(StringBuilder query, String key, String value) {
        if (query.length() > 0) {
            query.append("&");
        }
        query.append(key).append("=").append(UserCenterHttp.encode(value));
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
