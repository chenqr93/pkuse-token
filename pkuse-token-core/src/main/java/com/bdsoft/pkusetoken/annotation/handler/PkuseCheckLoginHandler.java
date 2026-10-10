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

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.annotation.PkuseCheckLogin;
import com.bdsoft.pkusetoken.stp.StpLogic;

import java.lang.reflect.AnnotatedElement;

/**
 * 注解 PkuseCheckLogin 的处理器
 *
 * @author click33
 * @since 2024/8/2
 */
public class PkuseCheckLoginHandler implements PkuseAnnotationHandlerInterface<PkuseCheckLogin> {

    @Override
    public Class<PkuseCheckLogin> getHandlerAnnotationClass() {
        return PkuseCheckLogin.class;
    }

    @Override
    public void checkMethod(PkuseCheckLogin at, AnnotatedElement element) {
        _checkMethod(at.type());
    }

    public static void _checkMethod(String type) {
        StpLogic stpLogic = PkuseManager.getStpLogic(type, false);

        stpLogic.checkLogin();
    }

}