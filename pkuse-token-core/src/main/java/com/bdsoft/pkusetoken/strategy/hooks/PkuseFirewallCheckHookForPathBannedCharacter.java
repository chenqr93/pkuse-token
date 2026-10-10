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
package com.bdsoft.pkusetoken.strategy.hooks;

import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.context.model.PkuseResponse;
import com.bdsoft.pkusetoken.exception.RequestPathInvalidException;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;

/**
 * 防火墙策略校验钩子函数：请求 path 禁止字符校验
 *
 * @author click33
 * @since 1.41.0
 */
public class PkuseFirewallCheckHookForPathBannedCharacter implements PkuseFirewallCheckHook {

    /**
     * 默认实例
     */
    public static PkuseFirewallCheckHookForPathBannedCharacter instance = new PkuseFirewallCheckHookForPathBannedCharacter();

    /**
     * 是否严格禁止出现百分号字符 % （默认：否）
     */
    public boolean bannedPercentage = false;

    /**
     * 重载配置
     * @param bannedPercentage 是否严格禁止出现百分号字符 % （默认：否）
     */
    public void resetConfig(boolean bannedPercentage) {
        this.bannedPercentage = bannedPercentage;
    }

    /**
     * 执行的方法
     *
     * @param req 请求对象
     * @param res 响应对象
     * @param extArg 预留扩展参数
     */
    @Override
    public void execute(PkuseRequest req, PkuseResponse res, Object extArg) {
        // 非可打印 ASCII 字符检查
        String requestPath = req.getRequestPath();
        if(PkuseFoxUtil.hasNonPrintableASCII(requestPath)) {
            throw new RequestPathInvalidException("请求 path 包含禁止字符：" + requestPath, requestPath);
        }
        if(bannedPercentage && requestPath.contains("%")) {
            throw new RequestPathInvalidException("请求 path 包含禁止字符 %：" + requestPath, requestPath);
        }
    }

}
