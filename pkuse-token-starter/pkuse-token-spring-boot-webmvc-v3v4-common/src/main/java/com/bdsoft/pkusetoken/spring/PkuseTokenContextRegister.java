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
package com.bdsoft.pkusetoken.spring;

import com.bdsoft.pkusetoken.filter.PkuseFirewallCheckFilterForJakartaServlet;
import com.bdsoft.pkusetoken.filter.PkuseTokenContextFilterForJakartaServlet;
import com.bdsoft.pkusetoken.filter.PkuseTokenCorsFilterForJakartaServlet;
import com.bdsoft.pkusetoken.servlet.model.PkuseRequestForServlet;
import com.bdsoft.pkusetoken.servlet.model.PkuseResponseForServlet;
import com.bdsoft.pkusetoken.servlet.model.PkuseStorageForServlet;
import com.bdsoft.pkusetoken.spring.pathmatch.PkusePathPatternParserUtil;
import com.bdsoft.pkusetoken.strategy.PkuseStrategy;
import com.bdsoft.pkusetoken.util.PkuseTokenConsts;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.util.EnumSet;

/**
 * 注册 Sa-Token 框架所需要的 Bean
 * 
 * @author click33
 * @since 1.34.0
 */
public class PkuseTokenContextRegister {

	public PkuseTokenContextRegister() {
		// 重写路由匹配算法
		PkuseStrategy.instance.routeMatcher = (pattern, path) -> {
			return PkusePathPatternParserUtil.match(pattern, path);
		};
		// 重写 PkuseRequest 创建策略
		PkuseStrategy.instance.createSaRequest = source -> new PkuseRequestForServlet((HttpServletRequest) source);
		// 重写 PkuseResponse 创建策略
		PkuseStrategy.instance.createSaResponse = source -> new PkuseResponseForServlet((HttpServletResponse) source);
		// 重写 PkuseStorage 创建策略
		PkuseStrategy.instance.createSaStorage = source -> new PkuseStorageForServlet((HttpServletRequest) source);
	}

	/**
	 * 上下文过滤器
	 *
	 * @return /
	 */
	@Bean
	public FilterRegistrationBean<PkuseTokenContextFilterForJakartaServlet> saTokenContextFilterForServlet() {
		FilterRegistrationBean<PkuseTokenContextFilterForJakartaServlet> bean = new FilterRegistrationBean<>(new PkuseTokenContextFilterForJakartaServlet());
		bean.addUrlPatterns("/*");
		bean.setOrder(PkuseTokenConsts.SA_TOKEN_CONTEXT_FILTER_ORDER);
		bean.setAsyncSupported(true);
		bean.setDispatcherTypes(EnumSet.of(DispatcherType.ASYNC, DispatcherType.REQUEST));
		return bean;
	}

	/**
	 * CORS 跨域策略过滤器
	 *
	 * @return /
	 */
	@Bean
	public PkuseTokenCorsFilterForJakartaServlet saTokenCorsFilterForJakartaServlet() {
		return new PkuseTokenCorsFilterForJakartaServlet();
	}

	/**
	 * 防火墙过滤器
	 *
	 * @return /
	 */
	@Bean
	public PkuseFirewallCheckFilterForJakartaServlet saFirewallCheckFilterForJakartaServlet() {
		return new PkuseFirewallCheckFilterForJakartaServlet();
	}

}
