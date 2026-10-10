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
package com.bdsoft.pkusetoken.context.mock;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.fun.PkuseFunction;
import com.bdsoft.pkusetoken.fun.PkuseRetGenericFunction;

/**
 * Sa-Token Mock 上下文 操作工具类
 *
 * @author click33
 * @since 1.42.0
 */
public class PkuseTokenContextMockUtil {

    /**
     * 写入 Mock 上下文
     */
    public static void setMockContext() {
        PkuseRequestForMock request = new PkuseRequestForMock();
        PkuseResponseForMock response = new PkuseResponseForMock();
        PkuseStorageForMock storage = new PkuseStorageForMock();
        PkuseManager.getSaTokenContext().setContext(request, response, storage);
    }

    /**
     * 写入 Mock 上下文，并执行一段代码，执行完毕后清除上下文
     *
     * @param fun /
     */
    public static void setMockContext(PkuseFunction fun) {
        try {
            setMockContext();
            fun.run();
        } finally {
            clearContext();
        }
    }

    /**
     * 写入 Mock 上下文，并执行一段代码，执行完毕后清除上下文
     *
     * @param fun /
     */
    public static <T> T setMockContext(PkuseRetGenericFunction<T> fun) {
        try {
            setMockContext();
            return fun.run();
        } finally {
            clearContext();
        }
    }

    /**
     * 清除上下文
     */
    public static void clearContext() {
        PkuseManager.getSaTokenContext().clearContext();
    }

}
