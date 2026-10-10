package com.pj.current;

import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 全局异常处理 
 */
@RestControllerAdvice
public class GlobalException {

	@ExceptionHandler
	public PkuseResult handlerException(Exception e, HttpServletRequest request, HttpServletResponse response) {
		e.printStackTrace();
		return PkuseResult.error(e.getMessage());
	}
	
}
