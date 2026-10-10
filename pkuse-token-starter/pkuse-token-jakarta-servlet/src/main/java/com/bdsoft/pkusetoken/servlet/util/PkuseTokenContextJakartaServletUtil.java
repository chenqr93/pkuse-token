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
package com.bdsoft.pkusetoken.servlet.util;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.context.model.PkuseResponse;
import com.bdsoft.pkusetoken.context.model.PkuseStorage;
import com.bdsoft.pkusetoken.context.model.PkuseTokenContextModelBox;
import com.bdsoft.pkusetoken.fun.PkuseFunction;
import com.bdsoft.pkusetoken.fun.PkuseRetGenericFunction;
import com.bdsoft.pkusetoken.strategy.PkuseStrategy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * PkuseTokenContext 上下文读写工具类
 *
 * @author click33
 * @since 1.42.0
 */
public class PkuseTokenContextJakartaServletUtil {

	/**
	 * 写入当前上下文
	 * @param request /
	 * @param response /
	 */
	public static void setContext(HttpServletRequest request, HttpServletResponse response) {
		PkuseRequest req = PkuseStrategy.instance.createSaRequest.apply(request);
		PkuseResponse res = PkuseStrategy.instance.createSaResponse.apply(response);
		PkuseStorage stg = PkuseStrategy.instance.createSaStorage.apply(request);
		PkuseManager.getSaTokenContext().setContext(req, res, stg);
	}

	/**
	 * 写入上下文对象, 并在执行函数后将其清除
	 * @param request /
	 * @param response /
	 * @param fun /
	 */
	public static void setContext(HttpServletRequest request, HttpServletResponse response, PkuseFunction fun) {
		try {
			setContext(request, response);
			fun.run();
		} finally {
			clearContext();
		}
	}

	/**
	 * 写入上下文对象, 并在执行函数后将其清除
	 *
	 * @param request /
	 * @param response /
	 * @param fun /
	 * @return /
	 * @param <T> /
	 */
	public static <T> T setContext(HttpServletRequest request, HttpServletResponse response, PkuseRetGenericFunction<T> fun) {
		try {
			setContext(request, response);
			return fun.run();
		} finally {
			clearContext();
		}
	}

	/**
	 * 清除当前上下文
	 */
	public static void clearContext() {
		PkuseManager.getSaTokenContext().clearContext();
	}

	/**
	 * 获取当前 ModelBox
	 * @return /
	 */
	public static PkuseTokenContextModelBox getModelBox() {
		return PkuseManager.getSaTokenContext().getModelBox();
	}

	/**
	 * 获取当前 Request
	 * @return /
	 */
	public static HttpServletRequest getRequest() {
		return (HttpServletRequest) getModelBox().getRequest().getSource();
	}

	/**
	 * 获取当前 Response
	 * @return /
	 */
	public static HttpServletResponse getResponse() {
		return (HttpServletResponse) getModelBox().getResponse().getSource();
	}

}
