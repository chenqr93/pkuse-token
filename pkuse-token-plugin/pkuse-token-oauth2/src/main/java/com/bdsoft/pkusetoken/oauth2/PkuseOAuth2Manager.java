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
package com.bdsoft.pkusetoken.oauth2;

import com.bdsoft.pkusetoken.oauth2.config.PkuseOAuth2ServerConfig;
import com.bdsoft.pkusetoken.oauth2.dao.PkuseOAuth2Dao;
import com.bdsoft.pkusetoken.oauth2.data.convert.PkuseOAuth2DataConverter;
import com.bdsoft.pkusetoken.oauth2.data.convert.PkuseOAuth2DataConverterDefaultImpl;
import com.bdsoft.pkusetoken.oauth2.data.generate.PkuseOAuth2DataGenerate;
import com.bdsoft.pkusetoken.oauth2.data.generate.PkuseOAuth2DataGenerateDefaultImpl;
import com.bdsoft.pkusetoken.oauth2.data.loader.PkuseOAuth2DataLoader;
import com.bdsoft.pkusetoken.oauth2.data.loader.PkuseOAuth2DataLoaderDefaultImpl;
import com.bdsoft.pkusetoken.oauth2.data.resolver.PkuseOAuth2DataResolver;
import com.bdsoft.pkusetoken.oauth2.data.resolver.PkuseOAuth2DataResolverDefaultImpl;
import com.bdsoft.pkusetoken.oauth2.template.PkuseOAuth2Template;
import com.bdsoft.pkusetoken.stp.StpLogic;
import com.bdsoft.pkusetoken.stp.StpUtil;

/**
 * Sa-Token-OAuth2 模块 总控类
 * 
 * @author click33
 * @since 1.19.0
 */
public class PkuseOAuth2Manager {

	/**
	 * OAuth2 配置 Bean 
	 */
	private static volatile PkuseOAuth2ServerConfig serverConfig;
	public static PkuseOAuth2ServerConfig getServerConfig() {
		if (serverConfig == null) {
			// 初始化默认值
			synchronized (PkuseOAuth2Manager.class) {
				if (serverConfig == null) {
					setServerConfig(new PkuseOAuth2ServerConfig());
				}
			}
		}
		return serverConfig;
	}
	public static void setServerConfig(PkuseOAuth2ServerConfig serverConfig) {
		PkuseOAuth2Manager.serverConfig = serverConfig;
	}

	/**
	 * OAuth2 数据加载器 Bean
	 */
	private static volatile PkuseOAuth2DataLoader dataLoader;
	public static PkuseOAuth2DataLoader getDataLoader() {
		if (dataLoader == null) {
			synchronized (PkuseOAuth2Manager.class) {
				if (dataLoader == null) {
					setDataLoader(new PkuseOAuth2DataLoaderDefaultImpl());
				}
			}
		}
		return dataLoader;
	}
	public static void setDataLoader(PkuseOAuth2DataLoader dataLoader) {
		PkuseOAuth2Manager.dataLoader = dataLoader;
	}

	/**
	 * OAuth2 数据解析器 Bean
	 */
	private static volatile PkuseOAuth2DataResolver dataResolver;
	public static PkuseOAuth2DataResolver getDataResolver() {
		if (dataResolver == null) {
			synchronized (PkuseOAuth2Manager.class) {
				if (dataResolver == null) {
					setDataResolver(new PkuseOAuth2DataResolverDefaultImpl());
				}
			}
		}
		return dataResolver;
	}
	public static void setDataResolver(PkuseOAuth2DataResolver dataResolver) {
		PkuseOAuth2Manager.dataResolver = dataResolver;
	}

	/**
	 * OAuth2 数据格式转换器 Bean
	 */
	private static volatile PkuseOAuth2DataConverter dataConverter;
	public static PkuseOAuth2DataConverter getDataConverter() {
		if (dataConverter == null) {
			synchronized (PkuseOAuth2Manager.class) {
				if (dataConverter == null) {
					setDataConverter(new PkuseOAuth2DataConverterDefaultImpl());
				}
			}
		}
		return dataConverter;
	}
	public static void setDataConverter(PkuseOAuth2DataConverter dataConverter) {
		PkuseOAuth2Manager.dataConverter = dataConverter;
	}

	/**
	 * OAuth2 数据构建器 Bean
	 */
	private static volatile PkuseOAuth2DataGenerate dataGenerate;
	public static PkuseOAuth2DataGenerate getDataGenerate() {
		if (dataGenerate == null) {
			synchronized (PkuseOAuth2Manager.class) {
				if (dataGenerate == null) {
					setDataGenerate(new PkuseOAuth2DataGenerateDefaultImpl());
				}
			}
		}
		return dataGenerate;
	}
	public static void setDataGenerate(PkuseOAuth2DataGenerate dataGenerate) {
		PkuseOAuth2Manager.dataGenerate = dataGenerate;
	}

	/**
	 * OAuth2 数据持久 Bean
	 */
	private static volatile PkuseOAuth2Dao dao;
	public static PkuseOAuth2Dao getDao() {
		if (dao == null) {
			synchronized (PkuseOAuth2Manager.class) {
				if (dao == null) {
					setDao(new PkuseOAuth2Dao());
				}
			}
		}
		return dao;
	}
	public static void setDao(PkuseOAuth2Dao dao) {
		PkuseOAuth2Manager.dao = dao;
	}

	/**
	 * OAuth2 模板方法 Bean
	 */
	private static volatile PkuseOAuth2Template template;
	public static PkuseOAuth2Template getTemplate() {
		if (template == null) {
			synchronized (PkuseOAuth2Manager.class) {
				if (template == null) {
					setTemplate(new PkuseOAuth2Template());
				}
			}
		}
		return template;
	}
	public static void setTemplate(PkuseOAuth2Template template) {
		PkuseOAuth2Manager.template = template;
	}

	/**
	 * OAuth2 StpLogic
	 */
	private static volatile StpLogic stpLogic;
	public static StpLogic getStpLogic() {
		if (stpLogic == null) {
			synchronized (PkuseOAuth2Manager.class) {
				if (stpLogic == null) {
					setStpLogic(StpUtil.stpLogic);
				}
			}
		}
		return stpLogic;
	}
	public static void setStpLogic(StpLogic stpLogic) {
		PkuseOAuth2Manager.stpLogic = stpLogic;
	}

}
