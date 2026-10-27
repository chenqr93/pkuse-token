package com.bdsoft.client.magic.controller;

import com.bdsoft.client.magic.po.MagicOpenClientPO;
import com.bdsoft.client.magic.service.MagicOpenClientService;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLoginParameter;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * magic-api 开放接口令牌签发：应用凭证 → 本平台 pkuse-token 令牌
 *
 * <p> 供「应用级」调用方换取调用 magic 接口所需的令牌（两类调用方统一入口）： </p>
 * <ul>
 *   <li><b>法治中台内部应用（含 pkuse-token）</b>：应用后端在平台登记 client/secret 后，
 *       以签名换取应用级令牌（服务间调用，不依赖用户登录态）；用户级调用直接走统一登录携带 satoken 即可；</li>
 *   <li><b>外部第三方应用</b>：同样在平台登记凭证，换取本平台基于 pkuse-token 签发的令牌。</li>
 * </ul>
 *
 * <p> 调用方凭证已提取为<b>动态管理模块</b>（表 s_open_client，管理接口 /open/clients，
 * 增删改即时生效、无需重启）；本接口按 clientId 实时查询已启用的登记信息校验签名。 </p>
 *
 * <p> <b>签名算法</b>（与 Pkuse-Token 参数签名一致，参见 client4 NoSdk 的 SsoSignUtil）： </p>
 * <pre>
 *   ① 参数（client、timestamp、nonce）按 key 字典序升序拼接为 k=v&amp;k=v；
 *   ② 末尾追加 &amp;key={secret}，整体取 MD5（小写）作为 sign；
 *   ③ timestamp 为毫秒时间戳（15 分钟有效窗口），nonce 为一次性随机串（防重放）。
 * </pre>
 *
 * <p> 签发的令牌是本应用（db7）的 pkuse-token 会话，loginId 形如 open:{client}，
 * 后续请求放入 satoken 请求头即可通过鉴权拦截器校验，magic 脚本中可通过 loginId 变量识别调用方；
 * 管理端禁用/删除调用方或重置密钥时会按 loginId 吊销其全部已签发令牌。 </p>
 *
 * <pre>
 *   调用示例：
 *   POST /open/token
 *   client=fz-app-demo&timestamp=1740000000000&nonce=abc123&sign={md5(...)}
 *
 *   返回：{ code:200, token:"xxx", loginId:"open:fz-app-demo", timeout:7200 }
 * </pre>
 */
@RestController
@RequestMapping("/open")
public class OpenTokenController {

    private static final Logger log = LoggerFactory.getLogger(OpenTokenController.class);

    /** 时间戳允许的最大偏差（与 PkuseSignConfig.timestampDisparity 默认值一致：15 分钟） */
    private static final long TIMESTAMP_DISPARITY = 1000L * 60 * 15;

    /** 开放调用会话的设备标识 */
    private static final String OPEN_DEVICE_ID = "open-api";

    private final MagicOpenClientService openClientService;

    /** nonce 一次性缓存（防重放；单机内存实现，集群部署时建议改为 Redis） */
    private final Map<String, Long> nonceCache = new ConcurrentHashMap<>();

    public OpenTokenController(MagicOpenClientService openClientService) {
        this.openClientService = openClientService;
    }

    /**
     * 签发开放接口令牌
     *
     * @param client    调用方标识（需在管理接口 /open/clients 登记并启用）
     * @param timestamp 毫秒时间戳（15 分钟有效窗口）
     * @param nonce     一次性随机串
     * @param sign      参数签名（MD5）
     */
    @PostMapping("/token")
    public PkuseResult token(String client, String timestamp, String nonce, String sign) {
        // 1. 调用方登记校验（动态查询 s_open_client，需已启用）
        MagicOpenClientPO openClient = openClientService.findEnabled(client);
        if (openClient == null) {
            return PkuseResult.error("调用方未登记或已禁用：client=" + client);
        }
        // 2. 签名与防重放校验
        if (!verifySign(client, timestamp, nonce, sign, openClient.getSecretKey())) {
            return PkuseResult.error("签名校验失败（检查 client/timestamp/nonce/sign 与密钥）");
        }
        // 3. 签发本平台令牌：loginId = open:{client}（脚本中据此识别调用方）
        String loginId = "open:" + client;
        StpUtil.login(loginId, new PkuseLoginParameter()
                .setDeviceId(OPEN_DEVICE_ID)
                .setTimeout(openClient.getTimeoutSeconds()));
        log.info("开放接口令牌已签发：client={}（loginId={}，有效期 {} 秒）", client, loginId, openClient.getTimeoutSeconds());
        return PkuseResult.ok("签发成功")
                .set("token", StpUtil.getTokenValue())
                .set("loginId", loginId)
                .set("timeout", openClient.getTimeoutSeconds());
    }

    /** 校验 timestamp 窗口、sign 签名与 nonce 一次性 */
    private boolean verifySign(String client, String timestamp, String nonce, String sign, String secret) {
        if (isBlank(client) || isBlank(timestamp) || isBlank(nonce) || isBlank(sign)) {
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
