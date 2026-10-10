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
package com.bdsoft.pkusetoken.plugin;

import com.bdsoft.pkusetoken.sign.annotation.handle.PkuseCheckSignHandler;
import com.bdsoft.pkusetoken.strategy.PkuseAnnotationStrategy;

/**
 * PkuseToken 插件安装：API 参数签名 组件
 *
 * @author click33
 * @since 1.43.0
 */
public class PkuseTokenPluginForSign implements PkuseTokenPlugin {

    @Override
    public void install() {
        // 安装 API 参数签名 鉴权注解
        PkuseAnnotationStrategy.instance.registerAnnotationHandler(new PkuseCheckSignHandler());
    }

}