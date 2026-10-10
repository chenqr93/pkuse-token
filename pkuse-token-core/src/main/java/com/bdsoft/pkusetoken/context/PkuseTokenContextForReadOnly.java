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

/**
 * Sa-Token 上下文处理器次级实现：只读上下文
 * 
 * @author click33
 * @since 1.42.0
 */
public interface PkuseTokenContextForReadOnly extends PkuseTokenContext {

	@Override
	default void setContext(PkuseRequest req, PkuseResponse res, PkuseStorage stg) {

	}

	@Override
	default void clearContext() {

	}

	@Override
	default PkuseTokenContextModelBox getModelBox() {
		return null;
	}

}
