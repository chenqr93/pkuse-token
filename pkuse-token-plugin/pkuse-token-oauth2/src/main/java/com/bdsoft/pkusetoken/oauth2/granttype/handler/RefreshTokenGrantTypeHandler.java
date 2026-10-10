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
import com.bdsoft.pkusetoken.oauth2.data.model.RefreshTokenModel;
import com.bdsoft.pkusetoken.oauth2.error.PkuseOAuth2ErrorCode;
import com.bdsoft.pkusetoken.oauth2.exception.PkuseOAuth2ClientModelException;
import com.bdsoft.pkusetoken.oauth2.exception.PkuseOAuth2RefreshTokenException;

import java.util.List;

/**
 * refresh_token grant_type 处理器
 *
 * @author click33
 * @since 1.39.0
 */
public class RefreshTokenGrantTypeHandler implements PkuseOAuth2GrantTypeHandlerInterface {

    @Override
    public String getHandlerGrantType() {
        return GrantType.refresh_token;
    }

    @Override
    public AccessTokenModel getAccessToken(PkuseRequest req, String clientId, List<String> scopes) {
        // 获取参数
        String refreshToken = req.getParamNotNull(PkuseOAuth2Consts.Param.refresh_token);

        // 校验：Refresh-Token 是否存在
        RefreshTokenModel rt = PkuseOAuth2Manager.getDao().getRefreshToken(refreshToken);
        PkuseOAuth2RefreshTokenException.throwBy(rt == null, "无效refresh_token: " + refreshToken, refreshToken, PkuseOAuth2ErrorCode.CODE_30111);

        // 校验：Refresh-Token 代表的 ClientId 与提供的 ClientId 是否一致
        PkuseOAuth2ClientModelException.throwBy( ! rt.clientId.equals(clientId), "无效client_id: " + clientId, clientId, PkuseOAuth2ErrorCode.CODE_30122);

        // 获取新 Access-Token
        AccessTokenModel accessTokenModel = PkuseOAuth2Manager.getDataGenerate().refreshAccessToken(refreshToken);
        return accessTokenModel;
    }

}