package com.bdsoft.usercenter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 接口文档配置（springdoc-openapi 扫描 Controller 生成 OpenAPI 3）
 *
 * <p> 文档入口：{@code http://localhost:9000/doc.html}（Knife4j UI）； </p>
 * <p> 原始描述：{@code /v3/api-docs/{group}}（供 Apifox/Postman 导入接口契约）。 </p>
 *
 * <p> 注意：/v3/api-docs 与 doc.html 不在登录拦截范围（SaTokenConfigure 仅拦截 /api/**）， </p>
 * <p> 生产环境通过 springdoc.api-docs.enabled=false 关闭文档端点。 </p>
 */
@Configuration
public class Knife4jConfiguration {

    /** 认证与 SSO（登录/注销/统一认证） */
    @Bean
    public GroupedOpenApi authGroup() {
        return GroupedOpenApi.builder()
                .group("1-认证与SSO")
                .pathsToMatch("/api/auth/**", "/sso/**")
                .build();
    }

    /** 用户/机构/区划 */
    @Bean
    public GroupedOpenApi orgGroup() {
        return GroupedOpenApi.builder()
                .group("2-用户与组织")
                .pathsToMatch("/api/user/**", "/api/org/**", "/api/region/**")
                .build();
    }

    /** 应用管理 */
    @Bean
    public GroupedOpenApi clientGroup() {
        return GroupedOpenApi.builder()
                .group("3-应用管理")
                .pathsToMatch("/api/client/**")
                .build();
    }

    /** 规则配置与仪表盘 */
    @Bean
    public GroupedOpenApi configGroup() {
        return GroupedOpenApi.builder()
                .group("4-配置与仪表盘")
                .pathsToMatch("/api/config/**", "/api/dashboard/**")
                .build();
    }

    /** 日志 */
    @Bean
    public GroupedOpenApi logGroup() {
        return GroupedOpenApi.builder()
                .group("5-日志")
                .pathsToMatch("/api/log/**")
                .build();
    }

    /** 开放接口（签名） */
    @Bean
    public GroupedOpenApi openGroup() {
        return GroupedOpenApi.builder()
                .group("9-开放接口（签名）")
                .pathsToMatch("/open/**")
                .build();
    }

    /** OpenAPI 文档元信息 */
    @Bean
    public OpenAPI userCenterOpenApi() {
        return new OpenAPI().info(new Info()
                .title("用户中心（统一认证中心）API")
                .description("统一登录 / SSO-Server / 用户·组织·区划 / 应用注册 / 登录与操作日志；"
                        + "开放接口（/open/**）走 MD5 参数签名（@PkuseCheckSign），其余接口调试时浏览器自动携带 satoken Cookie。")
                .version("1.0")
                .license(new License().name("Apache 2.0")));
    }
}
