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
package com.bdsoft.pkusetoken.spring.oauth2;

import com.bdsoft.pkusetoken.oauth2.PkuseOAuth2Manager;
import com.bdsoft.pkusetoken.oauth2.config.PkuseOAuth2ServerConfig;
import com.bdsoft.pkusetoken.oauth2.dao.PkuseOAuth2Dao;
import com.bdsoft.pkusetoken.oauth2.data.convert.PkuseOAuth2DataConverter;
import com.bdsoft.pkusetoken.oauth2.data.generate.PkuseOAuth2DataGenerate;
import com.bdsoft.pkusetoken.oauth2.data.loader.PkuseOAuth2DataLoader;
import com.bdsoft.pkusetoken.oauth2.data.resolver.PkuseOAuth2DataResolver;
import com.bdsoft.pkusetoken.oauth2.granttype.handler.PkuseOAuth2GrantTypeHandlerInterface;
import com.bdsoft.pkusetoken.oauth2.processor.PkuseOAuth2ServerProcessor;
import com.bdsoft.pkusetoken.oauth2.scope.handler.PkuseOAuth2ScopeHandlerInterface;
import com.bdsoft.pkusetoken.oauth2.strategy.PkuseOAuth2Strategy;
import com.bdsoft.pkusetoken.oauth2.template.PkuseOAuth2Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import java.util.List;


// 小提示：如果你在 idea 中运行源码时出现异常：java: 程序包com.bdsoft.pkusetoken.oauth2不存在。
// 在项目根目录进入 cmd，执行 mvn package 即可解决


/**
 * 注入 Sa-Token-OAuth2 所需要的组件
 * 
 * @author click33
 * @since 1.34.0
 */
@ConditionalOnClass(PkuseOAuth2Manager.class)
public class PkuseOAuth2BeanInject {

	/**
	 * 注入 OAuth2 配置对象
	 * 
	 * @param saOAuth2Config 配置对象 
	 */
	@Autowired(required = false)
	public void setSaOAuth2Config(PkuseOAuth2ServerConfig saOAuth2Config) {
		PkuseOAuth2Manager.setServerConfig(saOAuth2Config);
	}

	/**
	 * 注入 OAuth2 模板代码类
	 * 
	 * @param saOAuth2Template 模板代码类
	 */
	@Autowired(required = false)
	public void setSaOAuth2Template(PkuseOAuth2Template saOAuth2Template) {
		PkuseOAuth2Manager.setTemplate(saOAuth2Template);
	}

	/**
	 * 注入 OAuth2 请求处理器
	 *
	 * @param serverProcessor 请求处理器
	 */
	@Autowired(required = false)
	public void setSaOAuth2Template(PkuseOAuth2ServerProcessor serverProcessor) {
		PkuseOAuth2ServerProcessor.instance = serverProcessor;
	}

	/**
	 * 注入 OAuth2 数据加载器
	 *
	 * @param dataLoader /
	 */
	@Autowired(required = false)
	public void setSaOAuth2DataLoader(PkuseOAuth2DataLoader dataLoader) {
		PkuseOAuth2Manager.setDataLoader(dataLoader);
	}

	/**
	 * 注入 OAuth2 数据解析器 Bean
	 *
	 * @param dataResolver /
	 */
	@Autowired(required = false)
	public void setSaOAuth2DataResolver(PkuseOAuth2DataResolver dataResolver) {
		PkuseOAuth2Manager.setDataResolver(dataResolver);
	}

	/**
	 * 注入 OAuth2 数据格式转换器 Bean
	 *
	 * @param dataConverter /
	 */
	@Autowired(required = false)
	public void setSaOAuth2DataConverter(PkuseOAuth2DataConverter dataConverter) {
		PkuseOAuth2Manager.setDataConverter(dataConverter);
	}

	/**
	 * 注入 OAuth2 数据构建器 Bean
	 *
	 * @param dataGenerate /
	 */
	@Autowired(required = false)
	public void setSaOAuth2DataGenerate(PkuseOAuth2DataGenerate dataGenerate) {
		PkuseOAuth2Manager.setDataGenerate(dataGenerate);
	}

	/**
	 * 注入 OAuth2 数据持久 Bean
	 *
	 * @param dao /
	 */
	@Autowired(required = false)
	public void setSaOAuth2Dao(PkuseOAuth2Dao dao) {
		PkuseOAuth2Manager.setDao(dao);
	}

	/**
	 * 注入自定义 scope 处理器
	 *
	 * @param handlerList 自定义 scope 处理器集合
	 */
	@Autowired(required = false)
	public void setSaOAuth2ScopeHandler(List<PkuseOAuth2ScopeHandlerInterface> handlerList) {
		for (PkuseOAuth2ScopeHandlerInterface handler : handlerList) {
			PkuseOAuth2Strategy.instance.registerScopeHandler(handler);
		}
	}

	/**
	 * 注入自定义 grant_type 处理器
	 *
	 * @param handlerList 自定义 grant_type 处理器集合
	 */
	@Autowired(required = false)
	public void setSaOAuth2GrantTypeHandlerInterface(List<PkuseOAuth2GrantTypeHandlerInterface> handlerList) {
		for (PkuseOAuth2GrantTypeHandlerInterface handler : handlerList) {
			PkuseOAuth2Strategy.instance.registerGrantTypeHandler(handler);
		}
	}

}
