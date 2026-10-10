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
package com.bdsoft.pkusetoken.annotation.handler;

import com.bdsoft.pkusetoken.annotation.PkuseCheckHttpBasic;
import com.bdsoft.pkusetoken.httpauth.basic.PkuseHttpBasicUtil;

import java.lang.reflect.AnnotatedElement;

/**
 * 注解 PkuseCheckHttpBasic 的处理器
 *
 * @author click33
 * @since 2024/8/2
 */
public class PkuseCheckHttpBasicHandler implements PkuseAnnotationHandlerInterface<PkuseCheckHttpBasic> {

    @Override
    public Class<PkuseCheckHttpBasic> getHandlerAnnotationClass() {
        return PkuseCheckHttpBasic.class;
    }

    @Override
    public void checkMethod(PkuseCheckHttpBasic at, AnnotatedElement element) {
        _checkMethod(at.realm(), at.account());
    }

    public static void _checkMethod(String realm, String account) {
        PkuseHttpBasicUtil.check(realm, account);
    }

}