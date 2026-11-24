package com.bdsoft.authcenter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 接口文档配置（springdoc-openapi 扫描 Controller 生成 OpenAPI 3）
 *
 * <p> 文档入口：{@code http://localhost:9010/doc.html}（Knife4j UI）； </p>
 * <p> 原始描述：{@code /v3/api-docs/{group}}（供 Apifox/Postman 导入接口契约）。 </p>
 */
@Configuration
public class Knife4jConfiguration {

    /** 授权管理（菜单/角色/用户授权/应用） */
    @Bean
    public GroupedOpenApi authzGroup() {
        return GroupedOpenApi.builder()
                .group("1-授权管理")
                .pathsToMatch("/api/authz/menu/**", "/api/authz/role/**", "/api/authz/user/**", "/api/authz/app/**")
                .build();
    }

    /** 概览与日志 */
    @Bean
    public GroupedOpenApi dashboardGroup() {
        return GroupedOpenApi.builder()
                .group("2-概览与日志")
                .pathsToMatch("/api/authz/overview/**", "/api/authz/log/**", "/api/authz/cleanup/**")
                .build();
    }

    /** 授权数据开放接口（供业务应用拉取菜单/权限/角色） */
    @Bean
    public GroupedOpenApi openGroup() {
        return GroupedOpenApi.builder()
                .group("3-授权数据开放接口（签名）")
                .pathsToMatch("/open/authz/**")
                .build();
    }

    /** OpenAPI 文档元信息 */
    @Bean
    public OpenAPI authCenterOpenApi() {
        return new OpenAPI().info(new Info()
                .title("授权中心（RBAC 权限管理）API")
                .description("统一权限（RBAC）管理中心：菜单管理、角色管理、用户授权、授权数据分发；"
                        + "登录采用 simple 共享会话（用户中心签发的 satoken Cookie 同域共享），"
                        + "文档调试时浏览器自动携带 Cookie。")
                .version("1.0")
                .license(new License().name("Apache 2.0")));
    }
}
