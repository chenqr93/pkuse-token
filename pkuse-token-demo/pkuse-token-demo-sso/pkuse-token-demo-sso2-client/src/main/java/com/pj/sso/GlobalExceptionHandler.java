package com.pj.sso;

import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理 
 * @author click33
 *
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	// 全局异常拦截 
	@ExceptionHandler
	public PkuseResult handlerException(Exception e) {
		e.printStackTrace(); 
		return PkuseResult.error(e.getMessage());
	}
	
}
