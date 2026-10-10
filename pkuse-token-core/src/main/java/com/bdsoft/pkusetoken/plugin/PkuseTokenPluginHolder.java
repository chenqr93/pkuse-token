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
package com.bdsoft.pkusetoken.plugin;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.exception.PkuseTokenPluginException;
import com.bdsoft.pkusetoken.fun.hooks.PkuseTokenPluginHookFunction;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Sa-Token 插件管理器，管理所有插件的加载与卸载
 *
 * @author click33
 * @since 1.41.0
 */
public class PkuseTokenPluginHolder {

	/**
	 * 默认实例，非单例模式，可替换
	 */
	public static PkuseTokenPluginHolder instance = new PkuseTokenPluginHolder();


	// ------------------- 插件管理器初始化相关 -------------------

	/**
	 * 是否已经加载过插件
	 */
	public boolean isLoader = false;

	/**
	 * SPI 文件所在目录名称
	 */
	public String spiDir = "pkusetoken";

	/**
	 * 初始化加载所有插件（多次调用只会执行一次）
	 */
	public synchronized void init() {
		if(isLoader) {
			return;
		}
		loaderPlugins();
		isLoader = true;
	}

	/**
	 * 根据 SPI 机制加载所有插件
	 * <p>
	 *    加载所有 jar 下 /META-INF/pkusetoken/ 目录下 com.bdsoft.pkusetoken.plugin.PkuseTokenPlugin 文件指定的实现类
	 * </p>
	 */
	public synchronized void loaderPlugins() {
		PkuseManager.getLog().info("SPI plugin loading start ...");
		List<PkuseTokenPlugin> plugins = _loaderPluginsBySpi(PkuseTokenPlugin.class, spiDir);
		for (PkuseTokenPlugin plugin : plugins) {
			installPlugin(plugin);
		}
		PkuseManager.getLog().info("SPI plugin loading end ...");
	}

	/**
	 * 自定义 SPI 读取策略 （无状态函数）
	 * @param serviceInterface SPI 接口
	 * @param dirName 目录名称
	 * @return /
	 * @param <T> /
	 */
	protected <T> List<T> _loaderPluginsBySpi(Class<T> serviceInterface, String dirName) {
		String path = "META-INF/" + dirName + "/" + serviceInterface.getName();
		List<T> providers = new ArrayList<>();
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			Enumeration<URL> resources = classLoader.getResources(path);
			while (resources.hasMoreElements()) {
				URL url = resources.nextElement();
				try (InputStream is = url.openStream()) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					String line;
					while ((line = reader.readLine()) != null) {
						line = line.trim();
						// 忽略空行和注释行
						if (!line.isEmpty() && !line.startsWith("#")) {
							Class<?> clazz = Class.forName(line, true, classLoader);
							T instance = serviceInterface.cast(clazz.getDeclaredConstructor().newInstance());
							providers.add(instance);
						}
					}
				} catch (Exception e) {
					throw new PkuseTokenPluginException("SPI 插件加载失败: " + e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			throw new PkuseTokenPluginException("SPI 插件加载失败: " + e.getMessage(), e);
		}
		return providers;
	}



	// ------------------- 插件管理 -------------------

	/**
	 * 所有插件的集合
	 */
	private final List<PkuseTokenPlugin> pluginList = new ArrayList<>();

	/**
	 * 获取插件集合副本 (拷贝插件集合，而非每个插件实例)
	 * @return /
	 */
	public synchronized List<PkuseTokenPlugin> getPluginListCopy() {
		return new ArrayList<>(pluginList);
	}

	/**
	 * 判断是否已经安装了指定插件
	 *
	 * @param pluginClass 插件类型
	 * @return /
	 */
	public synchronized<T extends PkuseTokenPlugin> boolean isInstalledPlugin(Class<T> pluginClass) {
		for (PkuseTokenPlugin plugin : pluginList) {
			if (plugin.getClass().equals(pluginClass)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取指定类型的插件
	 * @param pluginClass /
	 * @return /
	 * @param <T> /
	 */
	public synchronized<T extends PkuseTokenPlugin> T getPlugin(Class<T> pluginClass) {
		for (PkuseTokenPlugin plugin : pluginList) {
			if (plugin.getClass().equals(pluginClass)) {
				return (T) plugin;
			}
		}
		return null;
	}

	/**
	 * 消费指定集合的钩子函数，返回消费的数量
	 * @param pluginClass /
	 * @param hooks /
	 * @param <T> /
	 */
	protected synchronized <T extends PkuseTokenPlugin> int _consumeHooks(List<PkuseTokenPluginHookModel<? extends PkuseTokenPlugin>> hooks, Class<T> pluginClass) {
		int consumeCount = 0;
		for (int i = 0; i < hooks.size(); i++) {
			PkuseTokenPluginHookModel<? extends PkuseTokenPlugin> model = hooks.get(i);
			if(model.listenerClass.equals(pluginClass)) {
				model.executeFunction.execute(getPlugin(pluginClass));
				hooks.remove(i);
				i--;
				consumeCount++;
			}
		}
		return consumeCount;
	}


	// ------------------- 插件 Install 与 Destroy -------------------

	/**
	 * 安装指定插件
	 * @param plugin /
	 */
	public synchronized PkuseTokenPluginHolder installPlugin(PkuseTokenPlugin plugin) {

		// 插件为空，拒绝安装
		if (plugin == null) {
			throw new PkuseTokenPluginException("插件不可为空");
		}

		// 插件已经被安装过了，拒绝再次安装
		if (isInstalledPlugin(plugin.getClass())) {
			throw new PkuseTokenPluginException("插件 [ " + plugin.getClass().getCanonicalName() + " ] 已安装，不可重复安装");
		}

		// 执行该插件的 install 前置钩子
		_consumeHooks(beforeInstallHooks, plugin.getClass());

		// 插件安装
		int consumeCount = _consumeHooks(installHooks, plugin.getClass());
		if (consumeCount == 0) {
			plugin.install();
		}

		// 执行该插件的 install 后置钩子
		_consumeHooks(afterInstallHooks, plugin.getClass());

		// 添加到插件集合
		pluginList.add(plugin);

		// 返回对象自身，支持连缀风格调用
		return this;
	}

	/**
	 * 安装指定插件，根据插件类型
	 * @param pluginClass /
	 */
	public synchronized<T extends PkuseTokenPlugin> PkuseTokenPluginHolder installPlugin(Class<T> pluginClass) {
		try {
			T plugin = pluginClass.getDeclaredConstructor().newInstance();
			return installPlugin(plugin);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new PkuseTokenPluginException(e);
		}
	}

	/**
	 * 卸载指定插件
	 * @param plugin /
	 */
	public synchronized PkuseTokenPluginHolder destroyPlugin(PkuseTokenPlugin plugin) {

		// 插件为空，拒绝卸载
		if (plugin == null) {
			throw new PkuseTokenPluginException("插件不可为空");
		}

		// 插件未被安装，拒绝卸载
		if (!isInstalledPlugin(plugin.getClass())) {
			throw new PkuseTokenPluginException("插件 [ " + plugin.getClass().getCanonicalName() + " ] 未安装，无法卸载");
		}

		// 执行该插件的 destroy 前置钩子
		_consumeHooks(beforeDestroyHooks, plugin.getClass());

		// 插件卸载
		int consumeCount = _consumeHooks(destroyHooks, plugin.getClass());
		if (consumeCount == 0) {
			plugin.destroy();
		}

		// 执行该插件的 destroy 后置钩子
		_consumeHooks(afterDestroyHooks, plugin.getClass());

		// 返回对象自身，支持连缀风格调用
		return this;
	}

	/**
	 * 卸载指定插件，根据插件类型
	 * @param pluginClass /
	 */
	public synchronized<T extends PkuseTokenPlugin> PkuseTokenPluginHolder destroyPlugin(Class<T> pluginClass) {
		return destroyPlugin(getPlugin(pluginClass));
	}


	// ------------------- 插件 Install 钩子 -------------------

	/**
	 * 插件 [ Install 钩子 ] 集合
	 */
	private final List<PkuseTokenPluginHookModel<? extends PkuseTokenPlugin>> installHooks = new ArrayList<>();

	/**
	 * 插件 [ Install 前置钩子 ] 集合
	 */
	private final List<PkuseTokenPluginHookModel<? extends PkuseTokenPlugin>> beforeInstallHooks = new ArrayList<>();

	/**
	 * 插件 [ Install 后置钩子 ] 集合
	 */
	private final List<PkuseTokenPluginHookModel<? extends PkuseTokenPlugin>> afterInstallHooks = new ArrayList<>();

	/**
	 * 注册指定插件的 [ Install 钩子 ]，1、同插件支持多次注册。2、如果插件已经安装完毕，则抛出异常。3、注册 Install 钩子的插件默认安装行为将不再执行
	 * @param listenerClass /
	 * @param executeFunction /
	 * @param <T> /
	 */
	public synchronized<T extends PkuseTokenPlugin> PkuseTokenPluginHolder onInstall(Class<T> listenerClass, PkuseTokenPluginHookFunction<T> executeFunction) {
		// 如果指定的插件已经安装完毕，则不再允许注册前置钩子函数
		if(isInstalledPlugin(listenerClass)) {
			throw new PkuseTokenPluginException("插件 [ " + listenerClass.getCanonicalName() + " ] 已安装完毕，不允许再注册 Install 钩子函数");
		}

		// 堆积到钩子函数集合
		installHooks.add(new PkuseTokenPluginHookModel<T>(listenerClass, executeFunction));

		// 返回对象自身，支持连缀风格调用
		return this;
	}

	/**
	 * 注册指定插件的 [ Install 前置钩子 ]，1、同插件支持多次注册。2、如果插件已经安装完毕，则抛出异常
	 * @param listenerClass /
	 * @param executeFunction /
	 * @param <T> /
	 */
	public synchronized<T extends PkuseTokenPlugin> PkuseTokenPluginHolder onBeforeInstall(Class<T> listenerClass, PkuseTokenPluginHookFunction<T> executeFunction) {
		// 如果指定的插件已经安装完毕，则不再允许注册前置钩子函数
		if(isInstalledPlugin(listenerClass)) {
			throw new PkuseTokenPluginException("插件 [ " + listenerClass.getCanonicalName() + " ] 已安装完毕，不允许再注册 Install 前置钩子函数");
		}

		// 堆积到钩子函数集合
		beforeInstallHooks.add(new PkuseTokenPluginHookModel<T>(listenerClass, executeFunction));

		// 返回对象自身，支持连缀风格调用
		return this;
	}

	/**
	 * 注册指定插件的 [ Install 后置钩子 ]，1、同插件支持多次注册。2、如果插件已经安装完毕，则立即执行该钩子函数
	 * @param listenerClass /
	 * @param executeFunction /
	 * @param <T> /
	 */
	public synchronized<T extends PkuseTokenPlugin> PkuseTokenPluginHolder onAfterInstall(Class<T> listenerClass, PkuseTokenPluginHookFunction<T> executeFunction) {
		// 如果指定的插件已经安装完毕，则立即执行该钩子函数
		if(isInstalledPlugin(listenerClass)) {
			executeFunction.execute(getPlugin(listenerClass));
			return this;
		}

		// 堆积到钩子函数集合
		afterInstallHooks.add(new PkuseTokenPluginHookModel<T>(listenerClass, executeFunction));

		// 返回对象自身，支持连缀风格调用
		return this;
	}


	// ------------------- 插件 Destroy 钩子 -------------------

	/**
	 * 插件 [ Destroy 钩子 ] 集合
	 */
	private final List<PkuseTokenPluginHookModel<? extends PkuseTokenPlugin>> destroyHooks = new ArrayList<>();

	/**
	 * 插件 [ Destroy 前置钩子 ] 集合
	 */
	private final List<PkuseTokenPluginHookModel<? extends PkuseTokenPlugin>> beforeDestroyHooks = new ArrayList<>();

	/**
	 * 插件 [ Destroy 后置钩子 ] 集合
	 */
	private final List<PkuseTokenPluginHookModel<? extends PkuseTokenPlugin>> afterDestroyHooks = new ArrayList<>();

	/**
	 * 注册指定插件的 [ Destroy 钩子 ]，1、同插件支持多次注册。2、注册 Destroy 钩子的插件默认卸载行为将不再执行
	 * @param listenerClass /
	 * @param executeFunction /
	 * @param <T> /
	 */
	public synchronized<T extends PkuseTokenPlugin> PkuseTokenPluginHolder onDestroy(Class<T> listenerClass, PkuseTokenPluginHookFunction<T> executeFunction) {
		destroyHooks.add(new PkuseTokenPluginHookModel<T>(listenerClass, executeFunction));

		// 返回对象自身，支持连缀风格调用
		return this;
	}

	/**
	 * 注册指定插件的 [ Destroy 前置钩子 ]，同插件支持多次注册
	 * @param listenerClass /
	 * @param executeFunction /
	 * @param <T> /
	 */
	public synchronized<T extends PkuseTokenPlugin> PkuseTokenPluginHolder onBeforeDestroy(Class<T> listenerClass, PkuseTokenPluginHookFunction<T> executeFunction) {
		beforeDestroyHooks.add(new PkuseTokenPluginHookModel<T>(listenerClass, executeFunction));

		// 返回对象自身，支持连缀风格调用
		return this;
	}

	/**
	 * 注册指定插件的 [ Destroy 后置钩子 ]，同插件支持多次注册
	 * @param listenerClass /
	 * @param executeFunction /
	 * @param <T> /
	 */
	public synchronized<T extends PkuseTokenPlugin> PkuseTokenPluginHolder onAfterDestroy(Class<T> listenerClass, PkuseTokenPluginHookFunction<T> executeFunction) {
		afterDestroyHooks.add(new PkuseTokenPluginHookModel<T>(listenerClass, executeFunction));

		// 返回对象自身，支持连缀风格调用
		return this;
	}

}
