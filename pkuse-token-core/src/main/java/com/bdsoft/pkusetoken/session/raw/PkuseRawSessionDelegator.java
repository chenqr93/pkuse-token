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
package com.bdsoft.pkusetoken.session.raw;

import com.bdsoft.pkusetoken.session.PkuseSession;

/**
 * PkuseSession 读写工具类 委托
 * 
 * @author click33
 * @since 1.42.0
 */
public class PkuseRawSessionDelegator {

	/**
	 * raw session 类型
	 */
	public String type;

	public PkuseRawSessionDelegator(String type) {
		this.type = type;
	}

	/**
	 * 判断：指定 PkuseSession 是否存在
	 *
	 * @param valueId /
	 * @return 是否存在
	 */
	public boolean isExists(Object valueId) {
		return PkuseRawSessionUtil.isExists(type, valueId);
	}

	/**
	 * 获取指定 PkuseSession 对象, 如果此 PkuseSession 尚未在 Cache 创建，isCreate 参数代表是否则新建并返回
	 *
	 * @param valueId /
	 * @param isCreate  如果此 PkuseSession 尚未在 DB 创建，是否新建并返回
	 * @return PkuseSession 对象
	 */
	public PkuseSession getSessionById(Object valueId, boolean isCreate) {
		return PkuseRawSessionUtil.getSessionById(type, valueId, isCreate);
	}

	/**
	 * 获取指定 PkuseSession, 如果此 PkuseSession 尚未在 DB 创建，则新建并返回
	 *
	 * @param valueId /
	 * @return PkuseSession 对象
	 */
	public PkuseSession getSessionById(Object valueId) {
		return PkuseRawSessionUtil.getSessionById(type, valueId);
	}

	/**
	 * 删除指定 PkuseSession
	 *
	 * @param valueId /
	 */
	public void deleteSessionById(Object valueId) {
		PkuseRawSessionUtil.deleteSessionById(type, valueId);
	}

}
