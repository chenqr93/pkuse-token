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
package com.bdsoft.pkusetoken.servlet.model;

import com.bdsoft.pkusetoken.context.model.PkuseResponse;
import com.bdsoft.pkusetoken.exception.PkuseTokenException;
import com.bdsoft.pkusetoken.servlet.error.PkuseServletErrorCode;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 对 PkuseResponse 包装类的实现（Jakarta-Servlet 版）
 *
 * @author click33
 * @since 1.34.0
 */
public class PkuseResponseForServlet implements PkuseResponse {

	/**
	 * 底层Request对象
	 */
	protected HttpServletResponse response;
	
	/**
	 * 实例化
	 * @param response response对象 
	 */
	public PkuseResponseForServlet(HttpServletResponse response) {
		this.response = response;
	}
	
	/**
	 * 获取底层源对象 
	 */
	@Override
	public Object getSource() {
		return response;
	}

	/**
	 * 设置响应状态码 
	 */
	@Override
	public PkuseResponse setStatus(int sc) {
		response.setStatus(sc);
		return this;
	}
	
	/**
	 * 在响应头里写入一个值 
	 */
	@Override
	public PkuseResponse setHeader(String name, String value) {
		response.setHeader(name, value);
		return this;
	}

	/**
	 * 在响应头里添加一个值 
	 * @param name 名字
	 * @param value 值 
	 * @return 对象自身 
	 */
	@Override
	public PkuseResponse addHeader(String name, String value) {
		response.addHeader(name, value);
		return this;
	}
	
	/**
	 * 重定向 
	 */
	@Override
	public Object redirect(String url) {
		try {
			response.sendRedirect(url);
		} catch (Exception e) {
			throw new PkuseTokenException(e).setCode(PkuseServletErrorCode.CODE_20002);
		}
		return null;
	}

	
}
