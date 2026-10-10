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
import com.bdsoft.pkusetoken.exception.FirewallCheckException;
import com.bdsoft.pkusetoken.router.PkuseHttpMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 防火墙策略校验钩子函数：请求 Method 检测
 *
 * @author click33
 * @since 1.41.0
 */
public class PkuseFirewallCheckHookForHttpMethod implements PkuseFirewallCheckHook {

    /**
     * 默认实例
     */
    public static PkuseFirewallCheckHookForHttpMethod instance = new PkuseFirewallCheckHookForHttpMethod();

    /**
     * 是否校验 请求Method，默认开启
     */
    public boolean isCheckMethod = true;

    /**
     * 允许的 HttpMethod 列表
     */
    public List<String> allowMethods = new ArrayList<>();

    public PkuseFirewallCheckHookForHttpMethod() {
        // 默认允许的 HttpMethod 列表
        allowMethods.add(PkuseHttpMethod.GET.name());
        allowMethods.add(PkuseHttpMethod.POST.name());
        allowMethods.add(PkuseHttpMethod.PUT.name());
        allowMethods.add(PkuseHttpMethod.DELETE.name());
        allowMethods.add(PkuseHttpMethod.HEAD.name());
        allowMethods.add(PkuseHttpMethod.OPTIONS.name());
        allowMethods.add(PkuseHttpMethod.PATCH.name());
        allowMethods.add(PkuseHttpMethod.TRACE.name());
        allowMethods.add(PkuseHttpMethod.CONNECT.name());
    }

    /**
     * 配置
     * @param isCheckMethod 是否校验 Method
     * @param methods 允许的 HttpMethod 列表 (先清空原来的，再添加上新的)
     */
    public void resetConfig(boolean isCheckMethod, String... methods) {
        this.isCheckMethod = isCheckMethod;
        this.allowMethods.clear();
        this.allowMethods.addAll(Arrays.asList(methods));
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
        if(isCheckMethod) {
            String method = req.getMethod();
            if( ! allowMethods.contains(method) ) {
                throw new FirewallCheckException("非法请求 Method：" + method);
            }
        }
    }

}
