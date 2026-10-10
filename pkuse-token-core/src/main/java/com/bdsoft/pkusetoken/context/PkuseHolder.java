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
package com.bdsoft.pkusetoken.context;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.application.PkuseApplication;
import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.context.model.PkuseResponse;
import com.bdsoft.pkusetoken.context.model.PkuseStorage;

/**
 * Sa-Token 上下文持有类，你可以通过此类快速获取当前环境下的 PkuseRequest、PkuseResponse、PkuseStorage、PkuseApplication 对象。
 *
 * @author click33
 * @since 1.18.0
 */
public class PkuseHolder {
	
	/**
	 * 获取当前请求的 PkuseTokenContext 上下文对象
	 * @see PkuseTokenContext
	 * 
	 * @return /
	 */
	public static PkuseTokenContext getContext() {
		return PkuseManager.getSaTokenContext();
	}

	/**
	 * 获取当前请求的 Request 包装对象
	 * @see PkuseRequest
	 * 
	 * @return /
	 */
	public static PkuseRequest getRequest() {
		return PkuseManager.getSaTokenContext().getRequest();
	}

	/**
	 * 获取当前请求的 Response 包装对象
	 * @see PkuseResponse
	 * 
	 * @return /
	 */
	public static PkuseResponse getResponse() {
		return PkuseManager.getSaTokenContext().getResponse();
	}

	/**
	 * 获取当前请求的 Storage 包装对象
	 * @see PkuseStorage
	 *
	 * @return /
	 */
	public static PkuseStorage getStorage() {
		return PkuseManager.getSaTokenContext().getStorage();
	}

	/**
	 * 获取全局 PkuseApplication 对象
	 * @see PkuseApplication
	 * 
	 * @return /
	 */
	public static PkuseApplication getApplication() {
		return PkuseApplication.defaultInstance;
	}

}
