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
package com.bdsoft.pkusetoken.sso;

import com.bdsoft.pkusetoken.sso.config.PkuseSsoClientConfig;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoServerConfig;

/**
 * Sa-Token-SSO 模块 总控类
 *
 * @author click33
 * @since 1.30.0
 */
public class PkuseSsoManager {

	/**
	 * Sso Server 端 配置 Bean
	 */
	private volatile static PkuseSsoServerConfig serverConfig;
	public static PkuseSsoServerConfig getServerConfig() {
		if (serverConfig == null) {
			synchronized (PkuseSsoManager.class) {
				if (serverConfig == null) {
					setServerConfig(new PkuseSsoServerConfig());
				}
			}
		}
		return serverConfig;
	}
	public static void setServerConfig(PkuseSsoServerConfig serverConfig) {
		PkuseSsoManager.serverConfig = serverConfig;
		// 如果配置了 is-check-sign=false，则打印一条警告日志
		if ( ! serverConfig.getIsCheckSign()) {
			printNoCheckSignWarningByStartup();
		}
	}

	/**
	 * Sso Client 端 配置 Bean
	 */
	private volatile static PkuseSsoClientConfig clientConfig;
	public static PkuseSsoClientConfig getClientConfig() {
		if (clientConfig == null) {
			synchronized (PkuseSsoManager.class) {
				if (clientConfig == null) {
					setClientConfig(new PkuseSsoClientConfig());
				}
			}
		}
		return clientConfig;
	}
	public static void setClientConfig(PkuseSsoClientConfig clientConfig) {
		PkuseSsoManager.clientConfig = clientConfig;
		// 如果配置了 is-check-sign=false，则打印一条警告日志
		if ( ! clientConfig.getIsCheckSign()) {
			printNoCheckSignWarningByStartup();
		}
	}

	// 在启动时检测到 sa-token.sso-[server/client].is-check-sign=false 时，输出警告信息
	public static void printNoCheckSignWarningByStartup() {
		System.err.println("-----------------------------------------------------------------------------");
		System.err.println("警告信息：");
		System.err.println("当前配置项 sa-token.sso-[server/client].is-check-sign=false 代表跳过 SSO 参数签名校验");
		System.err.println("此模式仅为方便本地调试使用，生产环境下请务必配置为 true （配置项默认为true）");
		System.err.println("-----------------------------------------------------------------------------");
	}

	// 在运行时检测到 sa-token.sso-[server/client].is-check-sign=false 时，输出警告信息
	public static void printNoCheckSignWarningByRuntime() {
		System.err.println("警告信息：当前配置项 sa-token.sso-[server/client].is-check-sign=false 已跳过参数签名校验，" +
				"此模式仅为方便本地调试使用，生产环境下请务必配置为 true （配置项默认为true）");
	}

}
