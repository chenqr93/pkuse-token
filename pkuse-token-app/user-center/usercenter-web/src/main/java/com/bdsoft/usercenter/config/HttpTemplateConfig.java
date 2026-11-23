package com.bdsoft.usercenter.config;

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
 * SDK 访问外部应用所需的 HTTP 请求处理器
 *
 * <p> Pkuse-Token 中凡需要 SDK 主动发起 HTTP 请求的场景，都必须向框架提供一个 PkuseHttpTemplate </p>
 * <p> 实现（框架默认实现会抛出「HTTP 请求处理器未实现」），例如认证中心的以下能力： </p>
 * <ul>
 *   <li>单点注销：向各应用推送 logoutCall 注销通知；</li>
 *   <li>消息推送：向应用推送消息（is-push=1 的应用）。</li>
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
