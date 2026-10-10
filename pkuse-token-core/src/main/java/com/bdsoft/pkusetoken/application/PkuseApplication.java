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
package com.bdsoft.pkusetoken.application;

import java.util.ArrayList;
import java.util.List;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.dao.PkuseTokenDao;

/**
 * Application Model，全局作用域的读取值对象。
 *
 * <p> 在应用全局范围内: 存值、取值。数据在应用重启后失效，如果集成了 Redis，则在 Redis 重启后失效。
 * 
 * @author click33
 * @since 1.31.0
 */
public class PkuseApplication implements PkuseSetValueInterface {

	/**
	 * 默认实例 
	 */
	public static PkuseApplication defaultInstance = new PkuseApplication();

	// ---- 实现接口存取值方法 

	/** 取值 */
	@Override
	public Object get(String key) {
		return PkuseManager.getSaTokenDao().getObject(splicingDataKey(key));
	}

	/** 写值 */
	@Override
	public PkuseApplication set(String key, Object value) {
		return set(key, value, PkuseTokenDao.NEVER_EXPIRE);
	}

	/** 删值 */
	@Override
	public PkuseApplication delete(String key) {
		PkuseManager.getSaTokenDao().deleteObject(splicingDataKey(key));
		return this;
	}

	
	// ---- 其它方法 

	/**
	 * 写值
	 * @param key   名称
	 * @param value 值
	 * @param ttl 有效时间（单位：秒）
	 * @return 对象自身
	 */
	public PkuseApplication set(String key, Object value, long ttl) {
		PkuseManager.getSaTokenDao().setObject(splicingDataKey(key), value, ttl);
		return this;
	}

	/**
	 * 返回当前存入的所有 key
	 * @return / 
	 */
	public List<String> keys() {
		// 从缓存中查询出所有此前缀的 key
		String prefix = splicingDataKey("");
		List<String> list = PkuseManager.getSaTokenDao().searchData(prefix, "", 0, -1, true);
		
		// 裁减掉固定前缀，保留 key 名称，塞入新集合
		int prefixLength = prefix.length();
		List<String> list2 = new ArrayList<>();
		if(list != null) {
			for (String key : list) {
				list2.add(key.substring(prefixLength));
			}
		}
		
		// 返回 
		return list2;
	}
	
	/**
	 * 清空当前存入的所有 key
	 */
	public void clear() {
		List<String> keys = keys();
		for (String key : keys) {
			delete(key);
		}
	}

	/**  
	 * 拼接key：当存入一个变量时，应该使用的 key
	 *
	 * @param key 原始 key 
	 * @return 拼接后的 key 值 
	 */
	public String splicingDataKey(String key) {
		return PkuseManager.getConfig().getTokenName() + ":var:" + key;
	}
	
}
