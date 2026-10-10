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
package com.bdsoft.pkusetoken.spring.sso;

import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoClientConfig;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoServerConfig;
import com.bdsoft.pkusetoken.sso.processor.PkuseSsoClientProcessor;
import com.bdsoft.pkusetoken.sso.processor.PkuseSsoServerProcessor;
import com.bdsoft.pkusetoken.sso.template.PkuseSsoClientTemplate;
import com.bdsoft.pkusetoken.sso.template.PkuseSsoServerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * 注入 Sa-Token SSO 所需要的 Bean
 * 
 * @author click33
 * @since 1.34.0
 */
@ConditionalOnClass(PkuseSsoManager.class)
public class PkuseSsoBeanInject {

	/**
	 * 注入 Sa-Token SSO Server 端 配置类
	 * 
	 * @param serverConfig 配置对象
	 */
	@Autowired(required = false)
	public void setSaSsoServerConfig(PkuseSsoServerConfig serverConfig) {
		PkuseSsoManager.setServerConfig(serverConfig);
	}

	/**
	 * 注入 Sa-Token SSO Client 端 配置类
	 *
	 * @param clientConfig 配置对象
	 */
	@Autowired(required = false)
	public void setSaSsoClientConfig(PkuseSsoClientConfig clientConfig) {
		PkuseSsoManager.setClientConfig(clientConfig);
	}

	/**
	 * 注入 SSO 模板代码类 (Server 端)
	 *
	 * @param ssoServerTemplate /
	 */
	@Autowired(required = false)
	public void setSaSsoServerTemplate(PkuseSsoServerTemplate ssoServerTemplate) {
		PkuseSsoServerProcessor.instance.ssoServerTemplate = ssoServerTemplate;
	}

	/**
	 * 注入 SSO 模板代码类 (Client 端)
	 *
	 * @param ssoClientTemplate /
	 */
	@Autowired(required = false)
	public void setSaSsoClientTemplate(PkuseSsoClientTemplate ssoClientTemplate) {
		PkuseSsoClientProcessor.instance.ssoClientTemplate = ssoClientTemplate;
	}

}
