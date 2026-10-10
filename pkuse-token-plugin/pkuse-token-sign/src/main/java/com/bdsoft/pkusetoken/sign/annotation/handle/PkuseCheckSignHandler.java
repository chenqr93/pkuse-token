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
package com.bdsoft.pkusetoken.sign.annotation.handle;

import com.bdsoft.pkusetoken.annotation.handler.PkuseAnnotationHandlerInterface;
import com.bdsoft.pkusetoken.sign.annotation.PkuseCheckSign;
import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.sign.template.PkuseSignMany;

import java.lang.reflect.AnnotatedElement;

/**
 * 注解 PkuseCheckSign 的处理器
 *
 * @author click33
 * @since 1.41.0
 */
public class PkuseCheckSignHandler implements PkuseAnnotationHandlerInterface<PkuseCheckSign> {

    @Override
    public Class<PkuseCheckSign> getHandlerAnnotationClass() {
        return PkuseCheckSign.class;
    }

    @Override
    public void checkMethod(PkuseCheckSign at, AnnotatedElement element) {
        _checkMethod(at.appid(), at.verifyParams());
    }

    public static void _checkMethod(String appid, String[] verifyParams) {
        PkuseRequest req = PkuseHolder.getRequest();
        // 如果 appid 为 #{} 格式，则从请求参数中获取
        if(appid.startsWith("#{") && appid.endsWith("}")) {
            String reqParamName = appid.substring(2, appid.length() - 1);
            appid = req.getParam(reqParamName);
        }
        PkuseSignMany.getSignTemplate(appid).checkRequest(req, verifyParams);
    }

}