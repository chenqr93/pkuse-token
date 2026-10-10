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
package com.bdsoft.satoken.integrate.configure;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bdsoft.satoken.exception.DisableServiceException;
import com.bdsoft.satoken.exception.NotHttpBasicAuthException;
import com.bdsoft.satoken.exception.NotLoginException;
import com.bdsoft.satoken.exception.NotPermissionException;
import com.bdsoft.satoken.exception.NotRoleException;
import com.bdsoft.satoken.exception.NotSafeException;
import com.bdsoft.satoken.exception.SameTokenInvalidException;
import com.bdsoft.satoken.util.SaResult;

/**
 * 全局异常处理 
 * @author click33
 *
 */
@RestControllerAdvice
public class HandlerException {

	// 未登录异常，code=401
	@ExceptionHandler(NotLoginException.class)
	public SaResult handlerNotLoginException(NotLoginException e) {
		return SaResult.error().setCode(401);
	}

	// 缺少角色异常，code=402
	@ExceptionHandler(NotRoleException.class)
	public SaResult handlerNotRoleException(NotRoleException e) {
		return SaResult.error().setCode(402);
	}

	// 缺少权限异常，code=403
	@ExceptionHandler(NotPermissionException.class)
	public SaResult handlerNotPermissionException(NotPermissionException e) {
		return SaResult.error().setCode(403);
	}

	// 二级认证失败，code=901
	@ExceptionHandler(NotSafeException.class)
	public SaResult handlerNotSafeException(NotSafeException e) {
		return SaResult.error().setCode(901);
	}

	// same-token 校验失败，code=902
	@ExceptionHandler(SameTokenInvalidException.class)
	public SaResult handlerSameTokenInvalidException(SameTokenInvalidException e) {
		return SaResult.error().setCode(902);
	}

	// Http Basic 校验失败，code=903
	@ExceptionHandler(NotHttpBasicAuthException.class)
	public SaResult handlerNotBasicAuthException(NotHttpBasicAuthException e) {
		return SaResult.error().setCode(903);
	}

	// 服务被封禁 ，code=904
	@ExceptionHandler(DisableServiceException.class)
	public SaResult handlerDisableServiceException(DisableServiceException e) {
		return SaResult.error().setCode(904);
	}
	
}
