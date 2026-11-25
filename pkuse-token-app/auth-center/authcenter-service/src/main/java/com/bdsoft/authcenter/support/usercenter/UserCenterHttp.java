package com.bdsoft.authcenter.support.usercenter;

import com.bdsoft.authcenter.common.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 用户中心 HTTP 客户端（JDK HttpClient，无第三方依赖）
 *
 * <p> 授权中心与用户中心仅通过 HTTP 接口交互（应用注册表拉取 / 用户查询）， </p>
 * <p> 响应统一为 PkuseResult JSON：{code, msg, data, ...}。 </p>
 */
@Component
public class UserCenterHttp {

    private static final Logger log = LoggerFactory.getLogger(UserCenterHttp.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * GET 请求并解析 JSON 响应
     *
     * @param url     完整地址（含查询参数）
     * @param headers 附加请求头（如透传 satoken）
     */
    public JsonNode get(String url, Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET();
            if (headers != null) {
                headers.forEach(builder::header);
            }
            HttpResponse<String> response =
                    httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new BizException("用户中心接口响应异常：HTTP " + response.statusCode() + " - " + url);
            }
            return objectMapper.readTree(response.body());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("用户中心接口调用失败：{} - {}", url, e.getMessage());
            throw new BizException("用户中心服务不可用：" + e.getMessage());
        }
    }

    /** 校验 PkuseResult 响应码并返回 data 节点（code != 200 时抛业务异常） */
    public JsonNode unwrap(JsonNode body, String action) {
        if (body == null) {
            throw new BizException("用户中心响应为空：" + action);
        }
        int code = body.path("code").asInt(500);
        if (code != 200) {
            throw new BizException("用户中心" + action + "失败（code=" + code + "）：" + body.path("msg").asText(""));
        }
        return body.get("data");
    }

    /** URL 编码（UTF-8） */
    public static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
