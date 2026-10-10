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
package com.bdsoft.pkusetoken.spring;

import com.bdsoft.pkusetoken.context.PkuseTokenContextForReadOnly;
import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.context.model.PkuseResponse;
import com.bdsoft.pkusetoken.context.model.PkuseStorage;
import com.bdsoft.pkusetoken.servlet.model.PkuseRequestForServlet;
import com.bdsoft.pkusetoken.servlet.model.PkuseResponseForServlet;
import com.bdsoft.pkusetoken.servlet.model.PkuseStorageForServlet;

/**
 * <h2> 此为低版本(<1.42.0) 的上下文处理方案，基于 Spring 内部工具类 RequestContextHolder 读写上下文，仅做留档，如无必要请勿使用 </h2>
 *
 * Sa-Token 上下文处理器 [ SpringBoot3 Jakarta Servlet 版 ]，在 SpringBoot3 中使用 Sa-Token 时，必须注入此实现类，否则会出现上下文无效异常
 * 
 * @author click33
 * @since 1.34.0
 */
public class PkuseTokenContextForSpringInJakartaServlet implements PkuseTokenContextForReadOnly {

	/**
	 * 获取当前请求的 Request 包装对象
	 */
	@Override
	public PkuseRequest getRequest() {
		return new PkuseRequestForServlet(SpringMVCUtil.getRequest());
	}

	/**
	 * 获取当前请求的 Response 包装对象
	 */
	@Override
	public PkuseResponse getResponse() {
		return new PkuseResponseForServlet(SpringMVCUtil.getResponse());
	}

	/**
	 * 获取当前请求的 Storage 包装对象
	 */
	@Override
	public PkuseStorage getStorage() {
		return new PkuseStorageForServlet(SpringMVCUtil.getRequest());
	}

	/**
	 * 判断：在本次请求中，此上下文是否可用。
	 */
	@Override
	public boolean isValid() {
		return SpringMVCUtil.isWeb();
	}

}
