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
package com.bdsoft.pkusetoken.sign.template;

import com.bdsoft.pkusetoken.fun.PkuseParamRetFunction;
import com.bdsoft.pkusetoken.sign.PkuseSignManager;
import com.bdsoft.pkusetoken.sign.config.PkuseSignConfig;
import com.bdsoft.pkusetoken.sign.error.PkuseSignErrorCode;
import com.bdsoft.pkusetoken.sign.exception.PkuseSignException;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;

/**
 * API 参数签名算法 - 多实例总控类
 *
 * @author click33
 * @since 1.41.0
 */
public class PkuseSignMany {

	/**
	 * 根据 appid 获取 PkuseSignConfig，允许自定义
	 */
	public static PkuseParamRetFunction<String, PkuseSignConfig> findSaSignConfigMethod = (appid) -> {
		return PkuseSignManager.getSignMany().get(appid);
	};

	/**
	 * 获取 PkuseSignTemplate，根据 appid
	 * @param appid /
	 * @return /
	 */
	public static PkuseSignTemplate getSignTemplate(String appid) {

		// appid 为空，返回全局默认 PkuseSignTemplate
		if(PkuseFoxUtil.isEmpty(appid)){
			return PkuseSignManager.getSaSignTemplate();
		}

		// 获取 PkuseSignConfig
		PkuseSignConfig config = findSaSignConfigMethod.run(appid);
		if(config == null){
			throw new PkuseSignException("未找到签名配置，appid=" + appid).setCode(PkuseSignErrorCode.CODE_12211);
		}

		// 创建 PkuseSignTemplate 并返回
		return new PkuseSignTemplate(config);
	}

}
