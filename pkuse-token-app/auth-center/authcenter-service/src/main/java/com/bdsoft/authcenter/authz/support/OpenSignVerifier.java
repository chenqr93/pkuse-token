package com.bdsoft.authcenter.authz.support;

import com.bdsoft.authcenter.usercenter.support.OpenSignUtil;
import com.bdsoft.pkusetoken.PkuseManager;
import org.springframework.stereotype.Component;

/**
 * 开放分发接口签名校验器（/open/authz/**）
 *
 * <p> 调用方为各接入应用后端，凭证为用户中心「应用管理」登记、并同步到授权中心 </p>
 * <p> 注册表缓存（ac 侧无应用表）的应用密钥。 </p>
 *
 * <p> <b>签名算法</b>：参数（client、loginId、timestamp、nonce）按 key 字典序升序拼接为 k=v&amp;k=v， </p>
 * <p> 末尾追加 &amp;key={secret} 取 MD5（小写）；timestamp 毫秒时间戳 15 分钟窗口（容忍 ±5 分钟偏差）， </p>
 * <p> nonce 一次性防重放（Redis 存储，TTL 与窗口一致）。 </p>
 */
@Component
public class OpenSignVerifier {

    /** 时间戳允许的最大偏差（15 分钟） */
    private static final long TIMESTAMP_DISPARITY = 1000L * 60 * 15;

    /** nonce 缓存键前缀 */
    private static final String NONCE_KEY_PREFIX = "authcenter:nonce:";

    /**
     * 校验签名（时间窗口 + sign 比对 + nonce 一次性）
     *
     * @param client    调用方应用标识（参与签名）
     * @param loginId   目标用户 ID（参与签名，防篡改）
     * @param timestamp 毫秒时间戳
     * @param nonce     一次性随机串
     * @param sign      参数签名（MD5）
     * @param secret    应用密钥（注册表缓存中的最新密钥）
     */
    public boolean verify(String client, String loginId, String timestamp, String nonce, String sign, String secret) {
        if (isBlank(client) || isBlank(loginId) || isBlank(timestamp) || isBlank(nonce) || isBlank(sign) || isBlank(secret)) {
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
        // sign = md5(client=..&loginId=..&nonce=..&timestamp=..&key=secret)，参数按字典序拼接
        String expected = OpenSignUtil.md5(
                "client=" + client
                        + "&loginId=" + loginId
                        + "&nonce=" + nonce
                        + "&timestamp=" + timestamp
                        + "&key=" + secret);
        if (!expected.equals(sign)) {
            return false;
        }
        return consumeNonce(nonce);
    }

    /**
     * 应用级签名校验（无目标用户；角色清单/绑定分发等应用维度端点使用）
     *
     * <p> 签名与用户中心开放接口同格式：sign = md5(client=..&nonce=..&timestamp=..&key=secret) </p>
     */
    public boolean verifyApp(String client, String timestamp, String nonce, String sign, String secret) {
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
        String expected = OpenSignUtil.md5(
                "client=" + client
                        + "&nonce=" + nonce
                        + "&timestamp=" + timestamp
                        + "&key=" + secret);
        if (!expected.equals(sign)) {
            return false;
        }
        return consumeNonce(nonce);
    }

    /** nonce 一次性（Redis，TTL=窗口时长；重复出现即重放） */
    private boolean consumeNonce(String nonce) {
        String key = NONCE_KEY_PREFIX + nonce;
        if (PkuseManager.getSaTokenDao().get(key) != null) {
            return false;
        }
        PkuseManager.getSaTokenDao().set(key, "1", TIMESTAMP_DISPARITY / 1000);
        return true;
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
