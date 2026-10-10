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

import com.bdsoft.pkusetoken.annotation.PkuseCheckHttpDigest;
import com.bdsoft.pkusetoken.exception.PkuseTokenException;
import com.bdsoft.pkusetoken.httpauth.digest.PkuseHttpDigestUtil;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;

import java.lang.reflect.AnnotatedElement;

/**
 * 注解 PkuseCheckHttpDigest 的处理器
 *
 * @author click33
 * @since 2024/8/2
 */
public class PkuseCheckHttpDigestHandler implements PkuseAnnotationHandlerInterface<PkuseCheckHttpDigest> {

    @Override
    public Class<PkuseCheckHttpDigest> getHandlerAnnotationClass() {
        return PkuseCheckHttpDigest.class;
    }

    @Override
    public void checkMethod(PkuseCheckHttpDigest at, AnnotatedElement element) {
        _checkMethod(at.username(), at.password(), at.realm(), at.value());
    }

    public static void _checkMethod(String username, String password, String realm, String value) {
        // 如果配置了 value，则以 value 优先
        if(PkuseFoxUtil.isNotEmpty(value)){
            String[] arr = value.split(":");
            if(arr.length != 2){
                throw new PkuseTokenException("注解参数配置错误，格式应如：username:password");
            }
            PkuseHttpDigestUtil.check(arr[0], arr[1]);
            return;
        }

        // 如果配置了 username，则分别获取参数
        if(PkuseFoxUtil.isNotEmpty(username)){
            PkuseHttpDigestUtil.check(username, password, realm);
            return;
        }

        // 都没有配置，则根据全局配置参数进行校验
        PkuseHttpDigestUtil.check();
    }

}