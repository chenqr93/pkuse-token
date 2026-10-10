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
package com.bdsoft.pkusetoken.integrate.configure;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bdsoft.pkusetoken.exception.DisableServiceException;
import com.bdsoft.pkusetoken.exception.NotHttpBasicAuthException;
import com.bdsoft.pkusetoken.exception.NotLoginException;
import com.bdsoft.pkusetoken.exception.NotPermissionException;
import com.bdsoft.pkusetoken.exception.NotRoleException;
import com.bdsoft.pkusetoken.exception.NotSafeException;
import com.bdsoft.pkusetoken.exception.SameTokenInvalidException;
import com.bdsoft.pkusetoken.util.PkuseResult;

/**
 * 全局异常处理 
 * @author click33
 *
 */
@RestControllerAdvice
public class HandlerException {

	// 未登录异常，code=401
	@ExceptionHandler(NotLoginException.class)
	public PkuseResult handlerNotLoginException(NotLoginException e) {
		return PkuseResult.error().setCode(401);
	}

	// 缺少角色异常，code=402
	@ExceptionHandler(NotRoleException.class)
	public PkuseResult handlerNotRoleException(NotRoleException e) {
		return PkuseResult.error().setCode(402);
	}

	// 缺少权限异常，code=403
	@ExceptionHandler(NotPermissionException.class)
	public PkuseResult handlerNotPermissionException(NotPermissionException e) {
		return PkuseResult.error().setCode(403);
	}

	// 二级认证失败，code=901
	@ExceptionHandler(NotSafeException.class)
	public PkuseResult handlerNotSafeException(NotSafeException e) {
		return PkuseResult.error().setCode(901);
	}

	// same-token 校验失败，code=902
	@ExceptionHandler(SameTokenInvalidException.class)
	public PkuseResult handlerSameTokenInvalidException(SameTokenInvalidException e) {
		return PkuseResult.error().setCode(902);
	}

	// Http Basic 校验失败，code=903
	@ExceptionHandler(NotHttpBasicAuthException.class)
	public PkuseResult handlerNotBasicAuthException(NotHttpBasicAuthException e) {
		return PkuseResult.error().setCode(903);
	}

	// 服务被封禁 ，code=904
	@ExceptionHandler(DisableServiceException.class)
	public PkuseResult handlerDisableServiceException(DisableServiceException e) {
		return PkuseResult.error().setCode(904);
	}
	
}
