package com.bdsoft.client.config;

import com.bdsoft.pkusetoken.http.PkuseHttpTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * SDK 访问认证中心所需的 HTTP 请求处理器
 *
 * <p> Pkuse-Token 中凡需要 SDK 主动发起 HTTP 请求调用认证中心的场景，都必须向框架提供一个 </p>
 * <p> PkuseHttpTemplate 实现（框架默认实现会抛出「HTTP 请求处理器未实现」），例如： </p>
 * <ul>
 *   <li>模式三：通过 HTTP 接口校验 ticket（sa-token.sso-client.is-http=true）；</li>
 *   <li>单点注销：客户端通过 HTTP 调用认证中心的注销接口；</li>
 *   <li>账号资料：客户端推送 userinfo 消息拉取用户信息。</li>
 * </ul>
 * <p> 这里使用 JDK HttpClient 实现，无第三方依赖。 </p>
 */
@Configuration
public class HttpTemplateConfig {

    @Bean
    public PkuseHttpTemplate pkuseHttpTemplate() {
        return new JdkHttpTemplate();
    }

    /** 基于 JDK HttpClient 的 HTTP 请求处理器（无第三方依赖） */
    static class JdkHttpTemplate implements PkuseHttpTemplate {

        private final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        @Override
        public String get(String url) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
            } catch (Exception e) {
                throw new IllegalStateException("HTTP GET 请求失败：" + url, e);
            }
        }

        @Override
        public String postByFormData(String url, Map<String, Object> params) {
            try {
                StringBuilder body = new StringBuilder();
                if (params != null) {
                    for (Map.Entry<String, Object> entry : params.entrySet()) {
                        if (body.length() > 0) {
                            body.append("&");
                        }
                        body.append(entry.getKey()).append("=")
                                .append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
                    }
                }
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                        .build();
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
            } catch (Exception e) {
                throw new IllegalStateException("HTTP POST 请求失败：" + url, e);
            }
        }
    }

}
