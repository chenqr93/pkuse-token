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
package com.bdsoft.pkusetoken.config;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.bdsoft.pkusetoken.error.PkuseErrorCode;
import com.bdsoft.pkusetoken.exception.PkuseTokenException;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;

/**
 * Sa-Token配置文件的构建工厂类
 *
 * <p> 用于手动读取配置文件初始化 PkuseTokenConfig 对象，只有在非IOC环境下你才会用到此类 </p>
 * 
 * @author click33
 * @since 1.10.0
 */
public class PkuseTokenConfigFactory {

	private PkuseTokenConfigFactory() {
	}
	
	/**
	 * 配置文件地址 
	 */
	public static String configPath = "sa-token.properties";

	/**
	 * 根据 configPath 路径获取配置信息
	 * 
	 * @return 一个PkuseTokenConfig对象
	 */
	public static PkuseTokenConfig createConfig() {
		return createConfig(configPath);
	}

	/**
	 * 根据指定路径路径获取配置信息 
	 * 
	 * @param path 配置文件路径 
	 * @return 一个 PkuseTokenConfig 对象
	 */
	public static PkuseTokenConfig createConfig(String path) {
		Map<String, String> map = readPropToMap(path);
		// if (map == null) {
			// throw new RuntimeException("找不到配置文件：" + configPath, null);
		// }
		return (PkuseTokenConfig) initPropByMap(map, new PkuseTokenConfig());
	}

	/**
	 * 工具方法: 将指定路径的properties配置文件读取到Map中 
	 * 
	 * @param propertiesPath 配置文件地址
	 * @return 一个Map
	 */
	private static Map<String, String> readPropToMap(String propertiesPath) {
		Map<String, String> map = new HashMap<>(16);
		try {
			InputStream is = PkuseTokenConfigFactory.class.getClassLoader().getResourceAsStream(propertiesPath);
			if (is == null) {
				return null;
			}
			Properties prop = new Properties();
			prop.load(is);
			for (String key : prop.stringPropertyNames()) {
				map.put(key, prop.getProperty(key));
			}
		} catch (IOException e) {
			throw new PkuseTokenException("配置文件(" + propertiesPath + ")加载失败", e).setCode(PkuseErrorCode.CODE_10021);
		}
		return map;
	}

	/**
	 * 工具方法: 将 Map 的值映射到一个 Model 上 
	 * 
	 * @param map 属性集合
	 * @param obj 对象, 或类型
	 * @return 返回实例化后的对象
	 */
	private static Object initPropByMap(Map<String, String> map, Object obj) {

		if (map == null) {
			map = new HashMap<>(16);
		}

		// 1、取出类型
		Class<?> cs;
		if (obj instanceof Class) {
			// 如果是一个类型，则将obj=null，以便完成静态属性反射赋值
			cs = (Class<?>) obj;
			obj = null;
		} else {
			// 如果是一个对象，则取出其类型
			cs = obj.getClass();
		}

		// 2、遍历类型属性，反射赋值
		for (Field field : cs.getDeclaredFields()) {
			String value = map.get(field.getName());
			if (value == null) {
				// 如果为空代表没有配置此项
				continue;
			}
			try {
				Object valueConvert = PkuseFoxUtil.getValueByType(value, field.getType());
				field.setAccessible(true);
				field.set(obj, valueConvert);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new PkuseTokenException("属性赋值出错：" + field.getName(), e).setCode(PkuseErrorCode.CODE_10022);
			}
		}
		return obj;
	}

}
