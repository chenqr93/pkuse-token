package com.pj.test;

import com.bdsoft.pkusetoken.oauth2.annotation.PkuseCheckAccessToken;
import com.bdsoft.pkusetoken.oauth2.annotation.PkuseCheckClientIdSecret;
import com.bdsoft.pkusetoken.oauth2.annotation.PkuseCheckClientToken;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth2 相关注解测试 Controller
 *
 * @author click33
 * @since 2024/8/25
 */
@RestController
@RequestMapping("/test")
public class TestController {

    // 测试：携带有效的 access_token 才可以进入请求
    // 你可以在请求参数中携带 access_token 参数，或者从请求头以 Authorization: bearer xxx 的形式携带
    @PkuseCheckAccessToken
    @RequestMapping("/checkAccessToken")
    public PkuseResult checkAccessToken() {
        return PkuseResult.ok("访问成功");
    }

    // 测试：携带有效的 access_token ，并且具备指定 scope 才可以进入请求
    @PkuseCheckAccessToken(scope = "userinfo")
    @RequestMapping("/checkAccessTokenScope")
    public PkuseResult checkAccessTokenScope() {
        return PkuseResult.ok("访问成功");
    }

    // 测试：携带有效的 access_token ，并且具备指定 scope 列表才可以进入请求
    @PkuseCheckAccessToken(scope = {"openid", "userinfo"})
    @RequestMapping("/checkAccessTokenScopeList")
    public PkuseResult checkAccessTokenScopeList() {
        return PkuseResult.ok("访问成功");
    }

    // 测试：携带有效的 client_token 才可以进入请求
    // 你可以在请求参数中携带 client_token 参数，或者从请求头以 Authorization: bearer xxx 的形式携带
    @PkuseCheckClientToken
    @RequestMapping("/checkClientToken")
    public PkuseResult checkClientToken() {
        return PkuseResult.ok("访问成功");
    }

    // 测试：携带有效的 client_token ，并且具备指定 scope 才可以进入请求
    @PkuseCheckClientToken(scope = "userinfo")
    @RequestMapping("/checkClientTokenScope")
    public PkuseResult checkClientTokenScope() {
        return PkuseResult.ok("访问成功");
    }

    // 测试：携带有效的 client_token ，并且具备指定 scope 列表才可以进入请求
    @PkuseCheckClientToken(scope = {"openid", "userinfo"})
    @RequestMapping("/checkClientTokenScopeList")
    public PkuseResult checkClientTokenScopeList() {
        return PkuseResult.ok("访问成功");
    }

    // 测试：携带有效的 client_id 和 client_secret 信息，才可以进入请求
    // 你可以在请求参数中携带 client_id 和 client_secret 参数，或者从请求头以 Authorization: Basic base64(client_id:client_secret) 的形式携带
    @PkuseCheckClientIdSecret
    @RequestMapping("/checkClientIdSecret")
    public PkuseResult checkClientIdSecret() {
        return PkuseResult.ok("访问成功");
    }

}