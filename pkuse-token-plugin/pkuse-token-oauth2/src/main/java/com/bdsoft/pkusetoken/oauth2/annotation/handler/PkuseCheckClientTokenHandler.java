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
package com.bdsoft.pkusetoken.oauth2.annotation.handler;

import com.bdsoft.pkusetoken.annotation.handler.PkuseAnnotationHandlerInterface;
import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.oauth2.PkuseOAuth2Manager;
import com.bdsoft.pkusetoken.oauth2.annotation.PkuseCheckClientToken;

import java.lang.reflect.AnnotatedElement;

/**
 * 注解 PkuseCheckAccessToken 的处理器
 *
 * @author click33
 * @since 1.39.0
 */
public class PkuseCheckClientTokenHandler implements PkuseAnnotationHandlerInterface<PkuseCheckClientToken> {

    @Override
    public Class<PkuseCheckClientToken> getHandlerAnnotationClass() {
        return PkuseCheckClientToken.class;
    }

    @Override
    public void checkMethod(PkuseCheckClientToken at, AnnotatedElement element) {
        _checkMethod(at.scope());
    }

    public static void _checkMethod(String[] scope) {
        String clientToken = PkuseOAuth2Manager.getDataResolver().readClientToken(PkuseHolder.getRequest());
        PkuseOAuth2Manager.getTemplate().checkClientTokenScope(clientToken, scope);
    }

}