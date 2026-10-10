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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 注册 Sa-Token SSO 所需要的 Bean
 *
 * @author click33
 * @since 1.34.0
 */
@ConditionalOnClass(PkuseSsoManager.class)
public class PkuseSsoBeanRegister {

	/**
	 * 获取 SSO Server 端 配置对象
	 * @return 配置对象
	 */
	@Bean
	@ConfigurationProperties(prefix = "sa-token.sso-server")
	public PkuseSsoServerConfig getSaSsoServerConfig() {
		return new PkuseSsoServerConfig();
	}

	/**
	 * 获取 SSO Client 端 配置对象
	 * @return 配置对象
	 */
	@Bean
	@ConfigurationProperties(prefix = "sa-token.sso-client")
	public PkuseSsoClientConfig getSaSsoClientConfig() {
		return new PkuseSsoClientConfig();
	}

	/**
	 * 获取 SSO Server 端 PkuseSsoServerTemplate
	 *
	 * @return /
	 */
	@Bean
	@ConditionalOnMissingBean(PkuseSsoServerTemplate.class)
	public PkuseSsoServerTemplate getSaSsoServerTemplate() {
		return PkuseSsoServerProcessor.instance.ssoServerTemplate;
	}

	/**
	 * 获取 SSO Client 端 PkuseSsoClientTemplate
	 *
	 * @return /
	 */
	@Bean
	@ConditionalOnMissingBean(PkuseSsoClientTemplate.class)
	public PkuseSsoClientTemplate getSaSsoClientTemplate() {
		return PkuseSsoClientProcessor.instance.ssoClientTemplate;
	}

}
