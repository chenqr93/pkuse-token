package com.bdsoft.client.magic;

import com.bdsoft.magicapi.core.context.MagicUser;
import com.bdsoft.magicapi.core.exception.MagicLoginException;
import com.bdsoft.magicapi.core.interceptor.AuthorizationInterceptor;
import com.bdsoft.pkusetoken.http.PkuseHttpTemplate;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLoginParameter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * magic-api 在线编辑器（Web UI）鉴权：AuthorizationInterceptor × Pkuse-Token
 *
 * <p> magic-api 的编辑器自身接口（登录、保存接口、刷新等）由 AuthorizationInterceptor 保护，
 * 编辑器登录成功后持有 {@link PkuseTokenRequestInterceptor#MAGIC_TOKEN_HEADER} 会话令牌。 </p>
 *
 * <p> <b>启用方式</b>：默认不生效（编辑器走 magic-api 内置静态账号，即 yml 中 magic-api.security 配置）；
 * 在 yml 中配置 {@code magic-auth.editor-auth: pkuse-token} 后本拦截器替换内置实现， </p>
 *
 * <p> 本应用没有自己的用户库（账号在认证中心），因此登录链路为： </p>
 *
 * <pre>
 *   1. login(username, password)：
 *      ① 委托认证中心校验账号密码：POST {server-url}/sso/doLogin（返回认证中心 token）；
 *      ② 用认证中心 token 换取用户信息：GET {server-url}/api/auth/me?satoken=xx
 *         （本应用与认证中心跨 Redis，无法本地解析认证中心 token，只能走 HTTP）；
 *      ③ 在本应用本地建立 pkuse-token 会话（db7），把本地 token 交给编辑器作为 Magic-Token。
 *      编辑器账号与统一登录共用认证中心用户（如 admin / 123456）。
 *   2. getUserByToken(token)：编辑器后续请求按本地会话校验（StpUtil.getLoginIdByToken）；
 *   3. requireLogin()=true：未登录禁止使用编辑器；
 *   4. logout(token)：编辑器退出时注销本地会话。
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "magic-auth.editor-auth", havingValue = "pkuse-token")
public class PkuseTokenAuthorizationInterceptor implements AuthorizationInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PkuseTokenAuthorizationInterceptor.class);

    /** 编辑器会话的设备标识（与页面端 SSO 会话区分，便于单独注销不影响页面端） */
    private static final String EDITOR_DEVICE_ID = "magic-editor";

    private final PkuseHttpTemplate httpTemplate;
    private final ObjectMapper objectMapper;
    /** 认证中心地址（与 SSO-Client 配置共用一个值） */
    private final String serverUrl;

    /** token → 编辑器用户缓存（仅用于界面展示用户名；token 有效性校验始终走 Redis） */
    private final Map<String, MagicUser> editorUsers = new ConcurrentHashMap<>();

    public PkuseTokenAuthorizationInterceptor(PkuseHttpTemplate httpTemplate,
                                              ObjectMapper objectMapper,
                                              @Value("${sa-token.sso-client.server-url}") String serverUrl) {
        this.httpTemplate = httpTemplate;
        this.objectMapper = objectMapper;
        this.serverUrl = serverUrl;
    }

    /** 编辑器要求登录（启用 pkuse-token 模式即强制登录） */
    @Override
    public boolean requireLogin() {
        return true;
    }

    /**
     * 编辑器登录：委托认证中心校验账号密码，成功后在本应用建立本地会话
     *
     * @return 编辑器会话用户（token 为本应用本地 token，编辑器自动放入 Magic-Token 头）
     */
    @Override
    public MagicUser login(String username, String password) throws MagicLoginException {
        try {
            // ① 认证中心校验账号密码（SSO 协议登录接口，data 为认证中心 token）
            String loginBody = httpTemplate.postByFormData(serverUrl + "/sso/doLogin",
                    Map.of("name", username, "pwd", password));
            JsonNode loginResult = objectMapper.readTree(loginBody);
            if (loginResult.path("code").asInt() != 200) {
                throw new MagicLoginException("登录失败：" + errorText(loginResult));
            }
            String authToken = loginResult.path("data").asText();

            // ② 以认证中心 token 换取用户信息（id / username / role 等）
            String meBody = httpTemplate.get(serverUrl + "/api/auth/me?satoken="
                    + URLEncoder.encode(authToken, StandardCharsets.UTF_8));
            JsonNode meResult = objectMapper.readTree(meBody);
            if (meResult.path("code").asInt() != 200) {
                throw new MagicLoginException("获取用户信息失败：" + errorText(meResult));
            }
            JsonNode user = meResult.path("data");
            String loginId = user.path("id").asText();

            // ③ 本应用建立本地会话（db7）：编辑器后续请求改用本地 token
            StpUtil.login(loginId, new PkuseLoginParameter().setDeviceId(EDITOR_DEVICE_ID));
            MagicUser editorUser = new MagicUser(loginId, user.path("username").asText(username), StpUtil.getTokenValue());
            editorUsers.put(editorUser.getToken(), editorUser);
            log.info("magic-api 编辑器登录成功：{}（loginId={}）", username, loginId);
            return editorUser;
        } catch (MagicLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("magic-api 编辑器登录异常", e);
            throw new MagicLoginException("登录异常：" + e.getMessage());
        }
    }

    /** 编辑器请求校验（Magic-Token 头）：按本应用本地会话校验有效性 */
    @Override
    public MagicUser getUserByToken(String token) throws MagicLoginException {
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            editorUsers.remove(token);
            throw new MagicLoginException("登录已失效，请重新登录");
        }
        // 缓存命中返回登录时的用户名；未命中（如服务重启后）退化为 loginId 展示，重新登录即恢复
        return editorUsers.computeIfAbsent(token,
                key -> new MagicUser(String.valueOf(loginId), String.valueOf(loginId), key));
    }

    /** 编辑器退出：注销本地会话并清理缓存 */
    @Override
    public void logout(String token) {
        editorUsers.remove(token);
        StpUtil.logoutByTokenValue(token);
    }

    /** 取返回报文中的错误信息（兼容 msg / message 两种字段名，均缺失时给出默认提示） */
    private static String errorText(JsonNode result) {
        String text = result.path("msg").asText(result.path("message").asText(""));
        return text.isBlank() ? "未知错误" : text;
    }
}
