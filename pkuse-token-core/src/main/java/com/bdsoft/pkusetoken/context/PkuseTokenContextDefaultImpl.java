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
 * Sa-Token 上下文处理器 [ 默认实现类 ]
 * 
 * <p>  
 * 	一般情况下框架会为你自动注入合适的上下文处理器，如果代码断点走到了此默认实现类，
 * 	说明你引入的依赖有问题或者错误的调用了 Sa-Token 的API， 请在 [ 在线开发文档 → 附录 → 常见问题排查 ] 中按照提示进行排查
 * </p>
 * 
 * @author click33
 * @since 1.16.0
 */
public class PkuseTokenContextDefaultImpl implements PkuseTokenContext {
	
	/**
	 * 默认的上下文处理器对象  
	 */
	public static PkuseTokenContextDefaultImpl defaultContext = new PkuseTokenContextDefaultImpl();

	/**
	 * 错误提示语
	 */
	public static final String ERROR_MESSAGE = "未能获取有效的上下文处理器";

	@Override
	public void setContext(PkuseRequest req, PkuseResponse res, PkuseStorage stg) {
		throw new PkuseTokenContextException(ERROR_MESSAGE).setCode(PkuseErrorCode.CODE_10001);
	}

	@Override
	public void clearContext() {
		throw new PkuseTokenContextException(ERROR_MESSAGE).setCode(PkuseErrorCode.CODE_10001);
	}

	@Override
	public boolean isValid() {
		throw new PkuseTokenContextException(ERROR_MESSAGE).setCode(PkuseErrorCode.CODE_10001);
	}

	@Override
	public PkuseTokenContextModelBox getModelBox() {
		throw new PkuseTokenContextException(ERROR_MESSAGE).setCode(PkuseErrorCode.CODE_10001);
	}

	@Override
	public PkuseRequest getRequest() {
		throw new PkuseTokenContextException(ERROR_MESSAGE).setCode(PkuseErrorCode.CODE_10001);
	}

	@Override
	public PkuseResponse getResponse() {
		throw new PkuseTokenContextException(ERROR_MESSAGE).setCode(PkuseErrorCode.CODE_10001);
	}

	@Override
	public PkuseStorage getStorage() {
		throw new PkuseTokenContextException(ERROR_MESSAGE).setCode(PkuseErrorCode.CODE_10001);
	}

}
