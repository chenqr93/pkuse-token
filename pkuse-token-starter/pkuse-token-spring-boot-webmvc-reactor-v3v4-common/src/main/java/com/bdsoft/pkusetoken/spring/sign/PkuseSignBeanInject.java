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
package com.bdsoft.pkusetoken.spring.sign;

import com.bdsoft.pkusetoken.sign.PkuseSignManager;
import com.bdsoft.pkusetoken.sign.config.PkuseSignConfig;
import com.bdsoft.pkusetoken.sign.config.PkuseSignManyConfigWrapper;
import com.bdsoft.pkusetoken.sign.template.PkuseSignTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * 注入 Sa-Token API 参数签名 所需要的 Bean
 * 
 * @author click33
 * @since 1.43.0
 */
@ConditionalOnClass(PkuseSignManager.class)
public class PkuseSignBeanInject {

	/**
	 * 注入 API 参数签名配置对象
	 *
	 * @param saSignConfig 配置对象
	 */
	@Autowired(required = false)
	public void setSignConfig(PkuseSignConfig saSignConfig) {
		PkuseSignManager.setConfig(saSignConfig);
	}

	/**
	 * 注入 API 参数签名配置对象
	 *
	 * @param signManyConfigWrapper 配置对象
	 */
	@Autowired(required = false)
	public void setSignManyConfig(PkuseSignManyConfigWrapper signManyConfigWrapper) {
		PkuseSignManager.setSignMany(signManyConfigWrapper.getSignMany());
	}

	/**
	 * 注入自定义的 参数签名 模版方法 Bean
	 *
	 * @param saSignTemplate 参数签名 Bean
	 */
	@Autowired(required = false)
	public void setSaSignTemplate(PkuseSignTemplate saSignTemplate) {
		PkuseSignManager.setSaSignTemplate(saSignTemplate);
	}

}
