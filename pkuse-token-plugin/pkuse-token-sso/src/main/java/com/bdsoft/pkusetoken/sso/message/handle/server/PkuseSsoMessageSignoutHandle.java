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
package com.bdsoft.pkusetoken.sso.message.handle.server;


import com.bdsoft.pkusetoken.sso.message.PkuseSsoMessage;
import com.bdsoft.pkusetoken.sso.message.handle.PkuseSsoMessageHandle;
import com.bdsoft.pkusetoken.sso.name.ParamName;
import com.bdsoft.pkusetoken.sso.template.PkuseSsoServerTemplate;
import com.bdsoft.pkusetoken.sso.template.PkuseSsoTemplate;
import com.bdsoft.pkusetoken.sso.util.PkuseSsoConsts;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLogoutParameter;
import com.bdsoft.pkusetoken.util.PkuseResult;

/**
 * SSO 消息处理器 - sso-server 端：处理 单点注销 的请求
 *
 * @author click33
 * @since 1.43.0
 */
public class PkuseSsoMessageSignoutHandle implements PkuseSsoMessageHandle {

    /**
     * 获取所要处理的消息类型
     *
     * @return /
     */
    public String getHandlerType() {
        return PkuseSsoConsts.MESSAGE_SIGNOUT;
    }

    /**
     * 执行方法
     *
     * @param ssoTemplate /
     * @param message /
     * @return /
     */
    public Object handle(PkuseSsoTemplate ssoTemplate, PkuseSsoMessage message) {

        // 1、获取对象
        PkuseSsoServerTemplate ssoServerTemplate = (PkuseSsoServerTemplate) ssoTemplate;
        ParamName paramName = ssoServerTemplate.paramName;

        // 2、判断当前是否开启了全局单点注销功能
        if( ! ssoServerTemplate.getServerConfig().getIsSlo()) {
            return PkuseResult.error("当前 sso-server 端未开启单点注销功能");
        }

        // 3、获取参数
        String client = message.getString(paramName.client);
        Object loginId = message.get(paramName.loginId);
        String deviceId = message.getString(paramName.deviceId);

        // 4、单点注销
        PkuseLogoutParameter logoutParameter = ssoServerTemplate.getStpLogicOrGlobal().createSaLogoutParameter().setDeviceId(deviceId);
        ssoServerTemplate.ssoLogout(loginId, logoutParameter, client);

        // 5、响应
        return PkuseResult.ok();
    }

}
