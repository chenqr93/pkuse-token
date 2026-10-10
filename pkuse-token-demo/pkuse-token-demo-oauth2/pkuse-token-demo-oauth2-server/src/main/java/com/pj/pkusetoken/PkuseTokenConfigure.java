package com.pj.pkusetoken;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.filter.PkuseServletFilter;
import com.bdsoft.pkusetoken.interceptor.PkuseInterceptor;
import com.bdsoft.pkusetoken.router.PkuseHttpMethod;
import com.bdsoft.pkusetoken.router.PkuseRouter;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * [Sa-Token 权限认证] 配置类 
 * @author click33
 *
 */
@Configuration
public class PkuseTokenConfigure implements WebMvcConfigurer {
	
	/**
	 * 注册 Sa-Token 拦截器打开注解鉴权功能  
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// 注册 Sa-Token 拦截器打开注解鉴权功能 
		registry.addInterceptor(new PkuseInterceptor()).addPathPatterns("/**");
	}

	/**
	 * 注册 [Sa-Token 全局过滤器]
	 */
	@Bean
	public PkuseServletFilter getSaServletFilter() {
		return new PkuseServletFilter()

				// 指定 [拦截路由] 与 [放行路由]
				.addInclude("/**").addExclude("/favicon.ico")

				// 认证函数: 每次请求执行
				.setAuth(obj -> {
					PkuseManager.getLog().debug("----- 请求path={}  提交token={}", PkuseHolder.getRequest().getRequestPath(), StpUtil.getTokenValue());
					// ...
				})

				// 异常处理函数：每次认证函数发生异常时执行此函数
				.setError(e -> {
					return PkuseResult.error(e.getMessage());
				})

				// 前置函数：在每次认证函数之前执行
				.setBeforeAuth(obj -> {
					PkuseHolder.getResponse()

							// ---------- 设置跨域响应头 ----------
							// 允许指定域访问跨域资源
							.setHeader("Access-Control-Allow-Origin", "*")
							// 允许所有请求方式
							.setHeader("Access-Control-Allow-Methods", "*")
							// 允许的header参数
							.setHeader("Access-Control-Allow-Headers", "*")
							// 有效时间
							.setHeader("Access-Control-Max-Age", "3600")
					;

					// 如果是预检请求，则立即返回到前端
					PkuseRouter.match(PkuseHttpMethod.OPTIONS)
							.free(r -> System.out.println("--------OPTIONS预检请求，不做处理"))
							.back();
				})
				;
	}

}
