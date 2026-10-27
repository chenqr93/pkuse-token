package com.bdsoft.client.magic;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * magic-api 接口鉴权（Pkuse-Token）配置
 *
 * <pre>
 * magic-auth:
 *   enabled: true                  # 鉴权总开关：false 时所有 magic 接口匿名放行
 *   anonymous-paths:               # 匿名放行的接口（Ant 通配，匹配完整请求路径）
 *     - /public/**
 *   editor-auth: builtin           # 编辑器鉴权方式：builtin=静态账号（magic-api.security 配置）/
 *                                  #            pkuse-token=认证中心统一账号（PkuseTokenAuthorizationInterceptor）
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "magic-auth")
public class MagicAuthProperties {

    /** 鉴权总开关：false 时所有 magic 接口匿名放行 */
    private boolean enabled = true;

    /** 匿名放行的接口路径（Ant 通配，如 /public/**） */
    private List<String> anonymousPaths = new ArrayList<>();

    /** 编辑器鉴权方式：builtin（magic-api.security 静态账号，默认）/ pkuse-token（认证中心统一账号） */
    private String editorAuth = "builtin";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getAnonymousPaths() {
        return anonymousPaths;
    }

    public void setAnonymousPaths(List<String> anonymousPaths) {
        this.anonymousPaths = anonymousPaths;
    }

    public String getEditorAuth() {
        return editorAuth;
    }

    public void setEditorAuth(String editorAuth) {
        this.editorAuth = editorAuth;
    }
}
