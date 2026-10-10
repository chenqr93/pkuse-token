package com.pj.test;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.servlet.util.PkuseTokenContextJakartaServletUtil;
import com.bdsoft.pkusetoken.spring.SpringMVCUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试专用Controller 
 * @author click33
 *
 */
@RestController
@RequestMapping("/test/")
public class TestController {

	// 测试   浏览器访问： http://localhost:8081/test/test
	@RequestMapping("test")
	public PkuseResult test() {
		System.out.println("------------进来了");
		System.out.println(SpringMVCUtil.getRequest());
		System.out.println(PkuseTokenContextJakartaServletUtil.getRequest());
		return PkuseResult.ok();
	}
	
	// 测试   浏览器访问： http://localhost:8081/test/test2
	@RequestMapping("test2")
	public PkuseResult test2() {
		return PkuseResult.ok();
	}

	// 测试   浏览器访问： http://localhost:8081/test/getRequestPath
	@RequestMapping("getRequestPath")
	public PkuseResult getRequestPath() {
		System.out.println("-------------- 测试请求 path 获取");
		System.out.println("request.getRequestURI() " + SpringMVCUtil.getRequest().getRequestURI());
		System.out.println("saRequest.getRequestPath() " + PkuseHolder.getRequest().getRequestPath());
		return PkuseResult.ok();
	}

}
