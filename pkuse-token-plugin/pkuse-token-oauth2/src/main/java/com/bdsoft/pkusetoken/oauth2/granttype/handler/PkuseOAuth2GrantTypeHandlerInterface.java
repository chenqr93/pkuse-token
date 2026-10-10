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
import com.bdsoft.pkusetoken.oauth2.data.model.AccessTokenModel;

import java.util.List;

/**
 * 所有 OAuth2 GrantType 处理器的父接口，如果要自定义 GrantType 处理器，必须实现此接口
 *
 * @author click33
 * @since 1.39.0
 */
public interface PkuseOAuth2GrantTypeHandlerInterface {

    /**
     * 获取所要处理的 GrantType
     *
     * @return /
     */
    String getHandlerGrantType();

    /**
     * 获取 AccessTokenModel 对象
     *
     * @param req /
     * @return /
     */
    AccessTokenModel getAccessToken(PkuseRequest req, String clientId, List<String> scopes);

}