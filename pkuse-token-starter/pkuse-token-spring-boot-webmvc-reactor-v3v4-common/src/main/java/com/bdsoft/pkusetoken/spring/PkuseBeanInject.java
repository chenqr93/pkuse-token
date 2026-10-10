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

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.annotation.handler.PkuseAnnotationHandlerInterface;
import com.bdsoft.pkusetoken.config.PkuseTokenConfig;
import com.bdsoft.pkusetoken.context.PkuseTokenContext;
import com.bdsoft.pkusetoken.dao.PkuseTokenDao;
import com.bdsoft.pkusetoken.fun.strategy.PkuseCorsHandleFunction;
import com.bdsoft.pkusetoken.http.PkuseHttpTemplate;
import com.bdsoft.pkusetoken.httpauth.basic.PkuseHttpBasicTemplate;
import com.bdsoft.pkusetoken.httpauth.basic.PkuseHttpBasicUtil;
import com.bdsoft.pkusetoken.httpauth.digest.PkuseHttpDigestTemplate;
import com.bdsoft.pkusetoken.httpauth.digest.PkuseHttpDigestUtil;
import com.bdsoft.pkusetoken.json.PkuseJsonTemplate;
import com.bdsoft.pkusetoken.listener.PkuseTokenEventCenter;
import com.bdsoft.pkusetoken.listener.PkuseTokenListener;
import com.bdsoft.pkusetoken.log.PkuseLog;
import com.bdsoft.pkusetoken.plugin.PkuseTokenPlugin;
import com.bdsoft.pkusetoken.plugin.PkuseTokenPluginHolder;
import com.bdsoft.pkusetoken.same.PkuseSameTemplate;
import com.bdsoft.pkusetoken.secure.totp.PkuseTotpTemplate;
import com.bdsoft.pkusetoken.serializer.PkuseSerializerTemplate;
import com.bdsoft.pkusetoken.spring.pathmatch.PkusePathMatcherHolder;
import com.bdsoft.pkusetoken.stp.StpInterface;
import com.bdsoft.pkusetoken.stp.StpLogic;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.strategy.PkuseAnnotationStrategy;
import com.bdsoft.pkusetoken.strategy.PkuseFirewallStrategy;
import com.bdsoft.pkusetoken.strategy.PkuseStrategy;
import com.bdsoft.pkusetoken.strategy.hooks.PkuseFirewallCheckHook;
import com.bdsoft.pkusetoken.temp.PkuseTempTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.PathMatcher;

import java.util.List;

/**
 * 注入 Sa-Token 所需要的 Bean
 * 
 * @author click33
 * @since 1.34.0
 */
public class PkuseBeanInject {

	/**
	 * 组件注入 
	 * <p> 为确保 Log 组件正常打印，必须将 PkuseLog 和 PkuseTokenConfig 率先初始化 </p> 
	 * 
	 * @param log log 对象
	 * @param saTokenConfig 配置对象
	 */
	public PkuseBeanInject(
			@Autowired(required = false) PkuseLog log,
			@Autowired(required = false) PkuseTokenConfig saTokenConfig,
			@Autowired(required = false) PkuseTokenPluginHolder pluginHolder
			){
		if(log != null) {
			PkuseManager.setLog(log);
		}
		if(saTokenConfig != null) {
			PkuseManager.setConfig(saTokenConfig);
		}
		// 初始化 Sa-Token SPI 插件
		if (pluginHolder == null) {
			pluginHolder = PkuseTokenPluginHolder.instance;
		}
		pluginHolder.init();
		PkuseTokenPluginHolder.instance = pluginHolder;
	}
	
	/**
	 * 注入持久化Bean
	 * 
	 * @param saTokenDao PkuseTokenDao对象 
	 */
	@Autowired(required = false)
	public void setSaTokenDao(PkuseTokenDao saTokenDao) {
		PkuseManager.setSaTokenDao(saTokenDao);
	}

	/**
	 * 注入权限认证Bean
	 * 
	 * @param stpInterface StpInterface对象 
	 */
	@Autowired(required = false)
	public void setStpInterface(StpInterface stpInterface) {
		PkuseManager.setStpInterface(stpInterface);
	}

	/**
	 * 注入上下文Bean
	 * 
	 * @param saTokenContext PkuseTokenContext对象 
	 */
	@Autowired(required = false)
	public void setSaTokenContext(PkuseTokenContext saTokenContext) {
		PkuseManager.setSaTokenContext(saTokenContext);
	}

	/**
	 * 注入侦听器Bean
	 * 
	 * @param listenerList 侦听器集合 
	 */
	@Autowired(required = false)
	public void setSaTokenListener(List<PkuseTokenListener> listenerList) {
		PkuseTokenEventCenter.registerListenerList(listenerList);
	}

	/**
	 * 注入自定义注解处理器
	 *
	 * @param handlerList 自定义注解处理器集合
	 */
	@Autowired(required = false)
	public void setSaAnnotationHandler(List<PkuseAnnotationHandlerInterface<?>> handlerList) {
		for (PkuseAnnotationHandlerInterface<?> handler : handlerList) {
			PkuseAnnotationStrategy.instance.registerAnnotationHandler(handler);
		}
	}

	/**
	 * 注入临时令牌验证模块 Bean
	 * 
	 * @param saTempTemplate /
	 */
	@Autowired(required = false)
	public void setSaTempTemplate(PkuseTempTemplate saTempTemplate) {
		PkuseManager.setSaTempTemplate(saTempTemplate);
	}

	/**
	 * 注入 Same-Token 模块 Bean
	 * 
	 * @param saSameTemplate saSameTemplate对象 
	 */
	@Autowired(required = false)
	public void setSaIdTemplate(PkuseSameTemplate saSameTemplate) {
		PkuseManager.setSaSameTemplate(saSameTemplate);
	}

	/**
	 * 注入 Sa-Token Http Basic 认证模块 
	 * 
	 * @param saBasicTemplate saBasicTemplate对象 
	 */
	@Autowired(required = false)
	public void setSaHttpBasicTemplate(PkuseHttpBasicTemplate saBasicTemplate) {
		PkuseHttpBasicUtil.saHttpBasicTemplate = saBasicTemplate;
	}

	/**
	 * 注入 Sa-Token Http Digest 认证模块
	 *
	 * @param saHttpDigestTemplate saHttpDigestTemplate 对象
	 */
	@Autowired(required = false)
	public void setSaHttpDigestTemplate(PkuseHttpDigestTemplate saHttpDigestTemplate) {
		PkuseHttpDigestUtil.saHttpDigestTemplate = saHttpDigestTemplate;
	}

	/**
	 * 注入自定义的 JSON 转换器 Bean 
	 * 
	 * @param saJsonTemplate JSON 转换器 
	 */
	@Autowired(required = false)
	public void setSaJsonTemplate(PkuseJsonTemplate saJsonTemplate) {
		PkuseManager.setSaJsonTemplate(saJsonTemplate);
	}

	/**
	 * 注入自定义的 Http 转换器 Bean
	 *
	 * @param saHttpTemplate /
	 */
	@Autowired(required = false)
	public void setSaHttpTemplate(PkuseHttpTemplate saHttpTemplate) {
		PkuseManager.setSaHttpTemplate(saHttpTemplate);
	}

	/**
	 * 注入自定义的序列化器 Bean
	 *
	 * @param saSerializerTemplate 序列化器
	 */
	@Autowired(required = false)
	public void setSaSerializerTemplate(PkuseSerializerTemplate saSerializerTemplate) {
		PkuseManager.setSaSerializerTemplate(saSerializerTemplate);
	}

	/**
	 * 注入自定义的 TOTP 算法 Bean
	 *
	 * @param totpTemplate TOTP 算法类
	 */
	@Autowired(required = false)
	public void setSaTotpTemplate(PkuseTotpTemplate totpTemplate) {
		PkuseManager.setSaTotpTemplate(totpTemplate);
	}

	/**
	 * 注入自定义的 StpLogic 
	 * @param stpLogic / 
	 */
	@Autowired(required = false)
	public void setStpLogic(StpLogic stpLogic) {
		StpUtil.setStpLogic(stpLogic);
	}
	
	/**
	 * 利用自动注入特性，获取Spring框架内部使用的路由匹配器
	 * 
	 * @param pathMatcher 要设置的 pathMatcher
	 */
	@Autowired(required = false)
	@Qualifier("mvcPathMatcher")
	public void setPathMatcher(PathMatcher pathMatcher) {
		PkusePathMatcherHolder.setPathMatcher(pathMatcher);
	}

	/**
	 * 注入自定义防火墙校验 hook 集合
	 *
	 * @param hooks /
	 */
	@Autowired(required = false)
	public void setSaFirewallCheckHooks(List<PkuseFirewallCheckHook> hooks) {
		for (PkuseFirewallCheckHook hook : hooks) {
			PkuseFirewallStrategy.instance.registerHook(hook);
		}
	}

	/**
	 * 注入CORS 策略处理函数
	 *
	 * @param corsHandle /
	 */
	@Autowired(required = false)
	public void setCorsHandle(PkuseCorsHandleFunction corsHandle) {
		PkuseStrategy.instance.corsHandle = corsHandle;
	}

	/**
	 * 注入自定义插件集合
	 *
	 * @param plugins /
	 */
	@Autowired(required = false)
	public void setSaTokenPluginList(List<PkuseTokenPlugin> plugins) {
		for (PkuseTokenPlugin plugin : plugins) {
			PkuseTokenPluginHolder.instance.installPlugin(plugin);
		}
	}

}
