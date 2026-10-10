/*
 * Copyright 2020-2099 sa-token.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bdsoft.pkusetoken.oauth2.scope.handler;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.jwt.PkuseJwtUtil;
import com.bdsoft.pkusetoken.jwt.error.PkuseJwtErrorCode;
import com.bdsoft.pkusetoken.jwt.exception.PkuseJwtException;
import com.bdsoft.pkusetoken.oauth2.PkuseOAuth2Manager;
import com.bdsoft.pkusetoken.oauth2.consts.PkuseOAuth2Consts;
import com.bdsoft.pkusetoken.oauth2.data.model.AccessTokenModel;
import com.bdsoft.pkusetoken.oauth2.data.model.ClientTokenModel;
import com.bdsoft.pkusetoken.oauth2.data.model.oidc.IdTokenModel;
import com.bdsoft.pkusetoken.oauth2.data.model.request.ClientIdAndSecretModel;
import com.bdsoft.pkusetoken.oauth2.exception.PkuseOAuth2Exception;
import com.bdsoft.pkusetoken.oauth2.scope.CommonScope;
import com.bdsoft.pkusetoken.sign.PkuseSignManager;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * id_token 权限处理器：在 AccessToken 扩展参数中追加 id_token 字段
 *
 * @author click33
 * @since 1.39.0
 */
public class OidcScopeHandler implements PkuseOAuth2ScopeHandlerInterface {

    public String getHandlerScope() {
        return CommonScope.OIDC;
    }

    @Override
    public void workAccessToken(AccessTokenModel at) {
        PkuseRequest req = PkuseHolder.getRequest();
        ClientIdAndSecretModel client = PkuseOAuth2Manager.getDataResolver().readClientIdAndSecret(req);

        // 基础参数
        IdTokenModel idToken = new IdTokenModel();
        idToken.iss = getIss();
        idToken.sub = at.loginId;
        idToken.aud = client.clientId;
        idToken.iat = System.currentTimeMillis() / 1000;
        idToken.exp = idToken.iat + PkuseOAuth2Manager.getServerConfig().getOidc().getIdTokenTimeout();
        idToken.authTime = PkuseOAuth2Manager.getStpLogic().getSessionByLoginId(at.loginId).getCreateTime() / 1000;
        idToken.nonce = getNonce();
        idToken.acr = null;
        idToken.amr = null;
        idToken.azp = client.clientId;

        // 额外参数
        idToken.extraData = new LinkedHashMap<>();
        idToken = workExtraData(idToken);

        // 构建 jwtIdToken
        String jwtIdToken = generateJwtIdToken(idToken);

        // 放入 AccessTokenModel
        at.extraData.put("id_token", jwtIdToken);
    }

    @Override
    public void workClientToken(ClientTokenModel ct) {

    }

    @Override
    public boolean refreshAccessTokenIsWork() {
        return true;
    }

    /**
     * 获取 iss
     * @return /
     */
    public String getIss() {
        // 如果开发者配置了 iss，则使用开发者配置的 iss
        String cfgIss = PkuseOAuth2Manager.getServerConfig().getOidc().getIss();
        if(PkuseFoxUtil.isNotEmpty(cfgIss)) {
            return cfgIss;
        }
        // 否则根据请求的 url 计算 iss
        //      例如请求 url 为： http://localhost:8081/abc/xyz?name=张三
        //      则计算的 iss 为： http://localhost:8081
        String urlString = PkuseHolder.getRequest().getUrl();
        try {
            URL url = new URL(urlString);
            String iss = url.getProtocol() + "://" + url.getHost();
            if(url.getPort() != -1) {
                iss += ":" + url.getPort();
            }
            return iss;
        } catch (MalformedURLException e) {
            throw new PkuseOAuth2Exception(e);
        }
    }

    /**
     * 获取 nonce
     * @return /
     */
    public String getNonce() {
        String nonce = PkuseHolder.getRequest().getParam(PkuseOAuth2Consts.Param.nonce);
        if(PkuseFoxUtil.isEmpty(nonce)) {
            // 通过 code 查找nonce
            // 为了避免其它 handler 可能会用到 nonce, 任由其自然过期，只取用不删除
            nonce = PkuseOAuth2Manager.getDao().getNonce(PkuseHolder.getRequest().getParam(PkuseOAuth2Consts.Param.code));
        }
        if(PkuseFoxUtil.isEmpty(nonce)) {
            nonce = PkuseFoxUtil.getRandomString(32);
        }
        PkuseSignManager.getSaSignTemplate().checkNonce(nonce);
        return nonce;
    }

    /**
     * 加工 IdTokenModel
     * @return /
     */
    public IdTokenModel workExtraData(IdTokenModel idToken) {
        // 留给开发者扩展
        return idToken;
    }

    /**
     * 将 IdTokenModel 转化为 Map 数据
     * @return /
     */
    public Map<String, Object> convertIdTokenToMap(IdTokenModel idToken) {
        // 基础参数
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("iss", idToken.iss);
        map.put("sub", idToken.sub);
        map.put("aud", idToken.aud);
        map.put("exp", idToken.exp);
        map.put("iat", idToken.iat);
        map.put("auth_time", idToken.authTime);
        map.put("nonce", idToken.nonce);
        map.put("acr", idToken.acr);
        map.put("amr", idToken.amr);
        map.put("azp", idToken.azp);

        // 移除 null 值
        idToken.extraData.entrySet().removeIf(entry -> entry.getValue() == null);

        // 扩展参数
        map.putAll(idToken.extraData);

        // 返回
        return map;
    }

    /**
     * 生成 jwt 格式的 id_token
     * @param idToken /
     * @return /
     */
    public String generateJwtIdToken(IdTokenModel idToken) {
        Map<String, Object> dataMap = convertIdTokenToMap(idToken);
        String keyt = PkuseOAuth2Manager.getStpLogic().getConfigOrGlobal().getJwtSecretKey();
        PkuseJwtException.throwByNull(keyt, "请配置jwt秘钥", PkuseJwtErrorCode.CODE_30205);
        return PkuseJwtUtil.createToken(dataMap, keyt);
    }

}