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
package com.bdsoft.pkusetoken.fun.strategy;

import com.bdsoft.pkusetoken.config.PkuseTokenConfig;

import java.util.function.Supplier;

/**
 * 函数式接口：获取 PkuseTokenConfig 的策略
 *
 * <p>  返回：PkuseTokenConfig 对象  </p>
 * <p>  由 {@link com.bdsoft.pkusetoken.strategy.PkuseStrategy#getSaTokenConfig} 持有，赋值后 {@link com.bdsoft.pkusetoken.PkuseManager#getConfig()} 每次调用都会执行此函数  </p>
 * <p>  注意：实现中请勿回调 {@link com.bdsoft.pkusetoken.PkuseManager#getConfig()}，否则会无限递归  </p>
 *
 * @author click33
 * @since 1.46.0
 */
@FunctionalInterface
public interface PkuseGetSaTokenConfigFunction extends Supplier<PkuseTokenConfig> {

}
