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
package com.bdsoft.pkusetoken.oauth2.granttype.handler;

import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.oauth2.PkuseOAuth2Manager;
import com.bdsoft.pkusetoken.oauth2.consts.GrantType;
import com.bdsoft.pkusetoken.oauth2.consts.PkuseOAuth2Consts;
import com.bdsoft.pkusetoken.oauth2.data.model.AccessTokenModel;
import com.bdsoft.pkusetoken.oauth2.data.model.request.ClientIdAndSecretModel;

import java.util.List;

/**
 * authorization_code grant_type 处理器
 *
 * @author click33
 * @since 1.39.0
 */
public class AuthorizationCodeGrantTypeHandler implements PkuseOAuth2GrantTypeHandlerInterface {

    @Override
    public String getHandlerGrantType() {
        return GrantType.authorization_code;
    }

    @Override
    public AccessTokenModel getAccessToken(PkuseRequest req, String clientId, List<String> scopes) {
        ClientIdAndSecretModel clientIdAndSecret = PkuseOAuth2Manager.getDataResolver().readClientIdAndSecret(req);
        String clientSecret = clientIdAndSecret.clientSecret;
        String code = req.getParamNotNull(PkuseOAuth2Consts.Param.code);
        String redirectUri = req.getParam(PkuseOAuth2Consts.Param.redirect_uri);

        // 校验参数
        PkuseOAuth2Manager.getTemplate().checkGainTokenParam(code, clientId, clientSecret, redirectUri);

        // 构建 Access-Token、返回
        return PkuseOAuth2Manager.getDataGenerate().generateAccessToken(code);
    }

}