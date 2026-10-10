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
package com.bdsoft.pkusetoken.sign;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.listener.PkuseTokenEventCenter;
import com.bdsoft.pkusetoken.sign.config.PkuseSignConfig;
import com.bdsoft.pkusetoken.sign.template.PkuseSignTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理 Sa-Token API 参数签名 所有全局组件
 *
 * @author click33
 * @since 1.43.0
 */
public class PkuseSignManager {

	/**
	 * API 参数签名 配置 Bean
	 */
	private static volatile PkuseSignConfig config;
	public static PkuseSignConfig getConfig() {
		if (config == null) {
			// 初始化默认值
			synchronized (PkuseSignManager.class) {
				if (config == null) {
					setConfig(new PkuseSignConfig());
				}
			}
		}
		return config;
	}
	public static void setConfig(PkuseSignConfig config) {
		PkuseSignManager.config = config;
	}

	/**
	 * API 签名配置 多实例 配置 Bean
	 */
	private static volatile Map<String, PkuseSignConfig> signMany;
	public static Map<String, PkuseSignConfig> getSignMany() {
		if (signMany == null) {
			// 初始化默认值
			synchronized (PkuseSignManager.class) {
				if (signMany == null) {
					setSignMany(new LinkedHashMap<>());
				}
			}
		}
		return signMany;
	}
	public static void setSignMany(Map<String, PkuseSignConfig> signMany) {
		PkuseSignManager.signMany = signMany;
	}

	/**
	 * API 参数签名
	 */
	private volatile static PkuseSignTemplate saSignTemplate;
	public static void setSaSignTemplate(PkuseSignTemplate saSignTemplate) {
		PkuseSignManager.saSignTemplate = saSignTemplate;
		PkuseTokenEventCenter.doRegisterComponent("PkuseSignTemplate", saSignTemplate);
	}
	public static PkuseSignTemplate getSaSignTemplate() {
		if (saSignTemplate == null) {
			synchronized (PkuseManager.class) {
				if (saSignTemplate == null) {
					PkuseSignManager.saSignTemplate = new PkuseSignTemplate();
				}
			}
		}
		return saSignTemplate;
	}

}
