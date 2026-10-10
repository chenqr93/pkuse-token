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
package com.bdsoft.pkusetoken.oauth2.granttype.handler;

import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.oauth2.PkuseOAuth2Manager;
import com.bdsoft.pkusetoken.oauth2.consts.GrantType;
import com.bdsoft.pkusetoken.oauth2.consts.PkuseOAuth2Consts;
import com.bdsoft.pkusetoken.oauth2.data.model.AccessTokenModel;
import com.bdsoft.pkusetoken.oauth2.data.model.request.RequestAuthModel;
import com.bdsoft.pkusetoken.oauth2.error.PkuseOAuth2ErrorCode;
import com.bdsoft.pkusetoken.oauth2.exception.PkuseOAuth2Exception;
import com.bdsoft.pkusetoken.oauth2.granttype.handler.model.PasswordAuthResult;
import com.bdsoft.pkusetoken.oauth2.strategy.PkuseOAuth2Strategy;
import com.bdsoft.pkusetoken.stp.StpUtil;

import java.util.List;

/**
 * password grant_type 处理器
 *
 * @author click33
 * @since 1.39.0
 */
public class PasswordGrantTypeHandler implements PkuseOAuth2GrantTypeHandlerInterface {

    @Override
    public String getHandlerGrantType() {
        return GrantType.password;
    }

    @Override
    public AccessTokenModel getAccessToken(PkuseRequest req, String clientId, List<String> scopes) {

        // 1、获取请求参数
        String username = req.getParamNotNull(PkuseOAuth2Consts.Param.username);
        String password = req.getParamNotNull(PkuseOAuth2Consts.Param.password);

        // 2、调用API 开始登录，如果没能成功登录，则直接退出
        PasswordAuthResult passwordAuthResult = loginByUsernamePassword(username, password);
        Object loginId = passwordAuthResult.getLoginId();
        if(loginId == null) {
            throw new PkuseOAuth2Exception("登录失败").setCode(PkuseOAuth2ErrorCode.CODE_30161);
        }

        // 3、构建 ra 对象
        RequestAuthModel ra = new RequestAuthModel();
        ra.clientId = clientId;
        ra.loginId = loginId;
        ra.scopes = scopes;

        // 4、生成 Access-Token
        AccessTokenModel at = PkuseOAuth2Manager.getDataGenerate().generateAccessToken(ra, true, atm -> atm.grantType = GrantType.password);
        return at;
    }

    /**
     * 根据 username、password 进行登录，如果登录失败请直接抛出异常或返回 loginId = null
     * @param username /
     * @param password /
     */
    public PasswordAuthResult loginByUsernamePassword(String username, String password) {
        System.err.println("警告信息：当前 password 认证模式，使用默认实现 (PkuseOAuth2Strategy.instance.doLoginHandle)，仅供开发测试");
        System.err.println("正式项目请重写 PasswordGrantTypeHandler 处理器 loginByUsernamePassword 方法");
        PkuseOAuth2Strategy.instance.doLoginHandle.apply(username, password);
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return new PasswordAuthResult(loginId);
    }

}