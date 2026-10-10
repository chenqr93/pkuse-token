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
package com.bdsoft.pkusetoken.sso.strategy;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.fun.PkuseParamRetFunction;
import com.bdsoft.pkusetoken.sso.function.SendRequestFunction;
import com.bdsoft.pkusetoken.sso.function.TicketResultHandleFunction;
import com.bdsoft.pkusetoken.util.PkuseResult;

import java.util.Map;

/**
 * Sa-Token SSO Client 相关策略
 *
 * @author click33
 * @since 1.43.0
 */
public class PkuseSsoClientStrategy {

    /**
     * 发送 Http 请求的处理函数
     */
    public SendRequestFunction sendRequest = url -> {
        return PkuseManager.getSaHttpTemplate().get(url);
    };

    /**
     * 自定义校验 ticket 返回值的处理逻辑 （每次从认证中心获取校验 ticket 的结果后调用）
     * <p> 参数：loginId, back
     * <p> 返回值：返回给前端的值
     */
    public TicketResultHandleFunction ticketResultHandle = null;

    /**
     * 转换：认证中心 centerId > 本地 loginId
     *
     * <p> 参数：认证中心 centerId
     * <p> 返回值：本地 loginId
     */
    public PkuseParamRetFunction<Object, Object> convertCenterIdToLoginId = (centerId) -> {
        return centerId;
    };

    /**
     * 转换：本地 loginId > 认证中心 centerId
     *
     * <p> 参数：本地 loginId
     * <p> 返回值：认证中心 centerId
     */
    public PkuseParamRetFunction<Object, Object> convertLoginIdToCenterId = (loginId) -> {
        return loginId;
    };

    /**
     * 发送 Http 请求，并将响应结果转换为 PkuseResult
     *
     * @param url 请求地址
     * @return 返回的结果
     */
    public PkuseResult requestAsSaResult(String url) {
        String body = sendRequest.apply(url);
        Map<String, Object> map = PkuseManager.getSaJsonTemplate().jsonToMap(body);
        return new PkuseResult(map);
    }

}
