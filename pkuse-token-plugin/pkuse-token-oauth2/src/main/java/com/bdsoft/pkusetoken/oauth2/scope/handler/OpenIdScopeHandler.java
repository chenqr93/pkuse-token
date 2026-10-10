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

import com.bdsoft.pkusetoken.oauth2.PkuseOAuth2Manager;
import com.bdsoft.pkusetoken.oauth2.consts.PkuseOAuth2Consts;
import com.bdsoft.pkusetoken.oauth2.data.model.AccessTokenModel;
import com.bdsoft.pkusetoken.oauth2.data.model.ClientTokenModel;
import com.bdsoft.pkusetoken.oauth2.scope.CommonScope;

/**
 * OpenId 权限处理器：在 AccessToken 扩展参数中追加 openid 字段
 *
 * @author click33
 * @since 1.39.0
 */
public class OpenIdScopeHandler implements PkuseOAuth2ScopeHandlerInterface {

    public String getHandlerScope() {
        return CommonScope.OPENID;
    }

    @Override
    public void workAccessToken(AccessTokenModel at) {
        at.extraData.put(PkuseOAuth2Consts.ExtraField.openid, PkuseOAuth2Manager.getDataLoader().getOpenid(at.clientId, at.loginId));
    }

    @Override
    public void workClientToken(ClientTokenModel ct) {

    }

}