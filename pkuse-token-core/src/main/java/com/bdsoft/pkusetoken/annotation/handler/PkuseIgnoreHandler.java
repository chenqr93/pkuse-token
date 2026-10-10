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

import com.bdsoft.pkusetoken.annotation.PkuseIgnore;
import com.bdsoft.pkusetoken.router.PkuseRouter;

import java.lang.reflect.AnnotatedElement;

/**
 * 注解 PkuseIgnore 的处理器
 * <h2> v1.43.0 版本起，PkuseIgnore 注解处理逻辑已转移到全局策略中，此处理器代码仅做留档 </h2>
 *
 * @author click33
 * @since 2024/8/2
 */
public class PkuseIgnoreHandler implements PkuseAnnotationHandlerInterface<PkuseIgnore> {

    @Override
    public Class<PkuseIgnore> getHandlerAnnotationClass() {
        return PkuseIgnore.class;
    }

    @Override
    public void checkMethod(PkuseIgnore at, AnnotatedElement element) {
        _checkMethod();
    }

    public static void _checkMethod() {
        PkuseRouter.stop();
    }

}