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
import com.bdsoft.pkusetoken.oauth2.annotation.PkuseCheckClientIdSecret;
import com.bdsoft.pkusetoken.oauth2.processor.PkuseOAuth2ServerProcessor;

import java.lang.reflect.AnnotatedElement;

/**
 * 注解 PkuseCheckClientSecret 的处理器
 *
 * @author click33
 * @since 1.39.0
 */
public class PkuseCheckClientIdSecretHandler implements PkuseAnnotationHandlerInterface<PkuseCheckClientIdSecret> {

    @Override
    public Class<PkuseCheckClientIdSecret> getHandlerAnnotationClass() {
        return PkuseCheckClientIdSecret.class;
    }

    @Override
    public void checkMethod(PkuseCheckClientIdSecret at, AnnotatedElement element) {
        _checkMethod();
    }

    public static void _checkMethod() {
        PkuseOAuth2ServerProcessor.instance.checkCurrClientSecret();
    }

}