package com.bdsoft.client.sso;

import com.bdsoft.client.config.SsoClientProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SSO 协议请求工具（手动实现，使用 JDK HttpClient，无第三方依赖）
 *
 * <p> 统一封装：向认证中心 /sso/pushS 推送消息（checkTicket / signout / userinfo）， </p>
 * <p> 请求参数自动附加 timestamp、nonce、sign 签名。 </p>
 */
@Component
public class SsoRequestUtil {

    private final SsoClientProperties properties;
    private final SsoSignUtil signUtil;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SsoRequestUtil(SsoClientProperties properties, SsoSignUtil signUtil) {
        this.properties = properties;
        this.signUtil = signUtil;
    }

    /**
     * 向认证中心推送消息（POST /sso/pushS）
     *
     * @param msgType     消息类型：checkTicket / signout / userinfo
     * @param bizParams   业务参数
     * @return 认证中心响应（Map 结构：code / msg / data / ...）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> pushMessage(String msgType, Map<String, String> bizParams) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("msgType", msgType);
        params.put("client", properties.getClientId());
        params.putAll(bizParams);
        signUtil.addSignParams(params);

        String body = buildFormBody(params);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.pushSUrl()))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return objectMapper.readValue(response.body(), Map.class);
    }

    /** 拼装表单请求体（值统一 URL 编码） */
    public static String buildFormBody(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(encodeUrl(entry.getValue()));
        }
        return sb.toString();
    }

    /** URL 参数拼接（值统一 URL 编码） */
    public static String buildUrl(String baseUrl, Map<String, String> params) {
        return baseUrl + "?" + buildFormBody(params);
    }

    public static String encodeUrl(String url) {
        return URLEncoder.encode(url == null ? "" : url, StandardCharsets.UTF_8);
    }

}
