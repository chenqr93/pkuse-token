package com.pj.test;

import com.bdsoft.pkusetoken.oauth2.PkuseOAuth2Manager;
import com.bdsoft.pkusetoken.oauth2.template.PkuseOAuth2Util;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 测试 OAuth2 相关 token 增删查
 *
 * @author click33
 * @since 2024/8/25
 */
@RestController
@RequestMapping("/test")
public class Test2Controller {

    // 测试：查询全部 Access-Token   --- http://localhost:8000/test/getAccessTokenValueList?clientId=1001&loginId=10001
    @RequestMapping("/getAccessTokenValueList")
    public PkuseResult getAccessTokenValueList(String clientId, long loginId) {
        List<String> accessTokenValueList = PkuseOAuth2Util.getAccessTokenValueList(clientId, loginId);
        return PkuseResult.data(accessTokenValueList);
    }

    // 测试：查询全部 Access-Token, 带过期时间   --- http://localhost:8000/test/getAccessTokenIndexMap?clientId=1001&loginId=10001
    @RequestMapping("/getAccessTokenIndexMap")
    public PkuseResult getAccessTokenIndexMap(String clientId, long loginId) {
        Map<String, Long> accessTokenIndexMap = PkuseOAuth2Manager.getDao().getAccessTokenIndexMap_FromAdjustAfter(clientId, loginId);
        return PkuseResult.data(accessTokenIndexMap);
    }

    // 测试：回收指定 Access-Token   --- http://localhost:8000/test/revokeAccessToken?access_token=xxxxxxxxxx
    @RequestMapping("/revokeAccessToken")
    public PkuseResult revokeAccessToken(String access_token) {
        PkuseOAuth2Util.revokeAccessToken(access_token);
        return PkuseResult.ok();
    }

    // 测试：回收全部 Access-Token   --- http://localhost:8000/test/revokeAccessTokenByIndex?clientId=1001&loginId=10001
    @RequestMapping("/revokeAccessTokenByIndex")
    public PkuseResult revokeAccessTokenByIndex(String clientId, long loginId) {
        PkuseOAuth2Util.revokeAccessTokenByIndex(clientId, loginId);
        return PkuseResult.ok();
    }


    // 测试：查询全部 Refresh-Token   --- http://localhost:8000/test/getRefreshTokenValueList?clientId=1001&loginId=10001
    @RequestMapping("/getRefreshTokenValueList")
    public PkuseResult getRefreshTokenValueList(String clientId, long loginId) {
        List<String> refreshTokenValueList = PkuseOAuth2Util.getRefreshTokenValueList(clientId, loginId);
        return PkuseResult.data(refreshTokenValueList);
    }

    // 测试：查询全部 Refresh-Token, 带过期时间   --- http://localhost:8000/test/getRefreshTokenIndexMap?clientId=1001&loginId=10001
    @RequestMapping("/getRefreshTokenIndexMap")
    public PkuseResult getRefreshTokenIndexMap(String clientId, long loginId) {
        Map<String, Long> refreshTokenIndexMap = PkuseOAuth2Manager.getDao().getRefreshTokenIndexMap_FromAdjustAfter(clientId, loginId);
        return PkuseResult.data(refreshTokenIndexMap);
    }

    // 测试：回收指定 Refresh-Token   --- http://localhost:8000/test/revokeRefreshToken?refresh_token=xxxxxxxxxx
    @RequestMapping("/revokeRefreshToken")
    public PkuseResult revokeRefreshToken(String refresh_token) {
        PkuseOAuth2Util.revokeRefreshToken(refresh_token);
        return PkuseResult.ok();
    }

    // 测试：回收全部 Refresh-Token   --- http://localhost:8000/test/revokeRefreshTokenByIndex?clientId=1001&loginId=10001
    @RequestMapping("/revokeRefreshTokenByIndex")
    public PkuseResult revokeRefreshTokenByIndex(String clientId, long loginId) {
        PkuseOAuth2Util.revokeRefreshTokenByIndex(clientId, loginId);
        return PkuseResult.ok();
    }


    // 测试：查询全部 Client-Token   --- http://localhost:8000/test/getClientTokenValueList?clientId=1001
    @RequestMapping("/getClientTokenValueList")
    public PkuseResult getClientTokenValueList(String clientId) {
        List<String> clientTokenValueList = PkuseOAuth2Util.getClientTokenValueList(clientId);
        return PkuseResult.data(clientTokenValueList);
    }

    // 测试：查询全部 Client-Token, 带过期时间   --- http://localhost:8000/test/getClientTokenIndexMap?clientId=1001&loginId=10001
    @RequestMapping("/getClientTokenIndexMap")
    public PkuseResult getClientTokenIndexMap(String clientId, long loginId) {
        Map<String, Long> rlientTokenIndexMap = PkuseOAuth2Manager.getDao().getClientTokenIndexMap_FromAdjustAfter(clientId, loginId);
        return PkuseResult.data(rlientTokenIndexMap);
    }

    // 测试：回收指定 Client-Token   --- http://localhost:8000/test/revokeClientToken?client_token=xxxxxxxxxxx
    @RequestMapping("/revokeClientToken")
    public PkuseResult revokeClientToken(String client_token) {
        PkuseOAuth2Util.revokeClientToken(client_token);
        return PkuseResult.ok();
    }

    // 测试：回收全部 Client-Token   --- http://localhost:8000/test/revokeClientTokenByIndex?clientId=1001
    @RequestMapping("/revokeClientTokenByIndex")
    public PkuseResult revokeClientTokenByIndex(String clientId) {
        PkuseOAuth2Util.revokeClientTokenByIndex(clientId);
        return PkuseResult.ok();
    }

}