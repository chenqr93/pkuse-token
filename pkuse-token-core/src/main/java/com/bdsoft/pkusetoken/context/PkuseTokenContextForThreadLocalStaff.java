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

import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.context.model.PkuseResponse;
import com.bdsoft.pkusetoken.context.model.PkuseStorage;
import com.bdsoft.pkusetoken.context.model.PkuseTokenContextModelBox;
import com.bdsoft.pkusetoken.error.PkuseErrorCode;
import com.bdsoft.pkusetoken.exception.PkuseTokenContextException;

/**
 * Sa-Token 上下文处理器 [ThreadLocal 版本] ---- 对象存储器
 *
 * <p> 一般情况下你不需要直接操作此类，因为框架的 starter 集成包里已经封装了完整的上下文操作 </p>
 *
 * @author click33
 * @since 1.16.0
 */
public class PkuseTokenContextForThreadLocalStaff {
	
	/**
	 * 基于 ThreadLocal 的 [ Box 存储器 ]
	 */
	public static ThreadLocal<PkuseTokenContextModelBox> modelBoxThreadLocal = new ThreadLocal<>();
	
	/**
	 * 初始化当前线程的 [ Box 存储器 ]
	 * @param request {@link PkuseRequest}
	 * @param response {@link PkuseResponse}
	 * @param storage {@link PkuseStorage}
	 */
	public static void setModelBox(PkuseRequest request, PkuseResponse response, PkuseStorage storage) {
		PkuseTokenContextModelBox bok = new PkuseTokenContextModelBox(request, response, storage);
		modelBoxThreadLocal.set(bok);
	}

	/**
	 * 清除当前线程的 [ Box 存储器 ]
	 */
	public static void clearModelBox() {
		modelBoxThreadLocal.remove();
	}

	/**
	 * 获取当前线程的 [ Box 存储器 ]
	 * @return /
	 */
	public static PkuseTokenContextModelBox getModelBoxOrNull() {
		return modelBoxThreadLocal.get();
	}
	
	/**
	 * 获取当前线程的 [ Box 存储器 ], 如果为空则抛出异常
	 * @return /
	 */
	public static PkuseTokenContextModelBox getModelBox() {
		PkuseTokenContextModelBox box = modelBoxThreadLocal.get();
		if(box ==  null) {
			throw new PkuseTokenContextException("PkuseTokenContext 上下文尚未初始化").setCode(PkuseErrorCode.CODE_10002);
		}
		return box;
	}

	/**
	 * 在当前线程的 PkuseRequest 包装对象
	 * 
	 * @return /
	 */
	public static PkuseRequest getRequest() {
		return getModelBox().getRequest();
	}

	/**
	 * 在当前线程的 PkuseResponse 包装对象
	 * 
	 * @return /
	 */
	public static PkuseResponse getResponse() {
		return getModelBox().getResponse();
	}

	/**
	 * 在当前线程的 PkuseStorage 存储器包装对象
	 * 
	 * @return /
	 */
	public static PkuseStorage getStorage() {
		return getModelBox().getStorage();
	}


}
