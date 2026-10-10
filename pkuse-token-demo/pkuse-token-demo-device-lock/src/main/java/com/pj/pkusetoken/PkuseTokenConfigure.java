package com.pj.pkusetoken;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.filter.PkuseServletFilter;
import com.bdsoft.pkusetoken.interceptor.PkuseInterceptor;
import com.bdsoft.pkusetoken.router.PkuseHttpMethod;
import com.bdsoft.pkusetoken.router.PkuseRouter;
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
        		.addInclude("/**")// .addExclude("/favicon.ico")
        		
        		// 认证函数: 每次请求执行 
        		.setAuth(obj -> {
        			// 输出 API 请求日志，方便调试代码 
//        			 PkuseManager.getLog().debug("----- 请求path={}  提交token={}", PkuseHolder.getRequest().getRequestPath(), StpUtil.getTokenValue());
                   
        		})
        		
        		// 异常处理函数：每次认证函数发生异常时执行此函数 
        		.setError(e -> {
        			System.out.println("---------- sa全局异常 ");
					e.printStackTrace();
        			return PkuseResult.error(e.getMessage());
        		})
        		
        		// 前置函数：在每次认证函数之前执行
        		.setBeforeAuth(obj -> {
        			// ---------- 设置一些安全响应头 ----------
        			PkuseHolder.getResponse()
        			// 服务器名称 
        			.setServer("sa-server")
        			// 是否可以在iframe显示视图： DENY=不可以 | SAMEORIGIN=同域下可以 | ALLOW-FROM uri=指定域名下可以 
        			.setHeader("X-Frame-Options", "SAMEORIGIN")
        			// 是否启用浏览器默认XSS防护： 0=禁用 | 1=启用 | 1; mode=block 启用, 并在检查到XSS攻击时，停止渲染页面
        			.setHeader("X-XSS-Protection", "1; mode=block")
        			// 禁用浏览器内容嗅探 
        			.setHeader("X-Content-Type-Options", "nosniff")
        			
        			// ---------- 设置跨域响应头 ----------
        			// 允许指定域访问跨域资源
        			.setHeader("Access-Control-Allow-Origin", "*")
        			// 允许所有请求方式
        			.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE")
        			// 有效时间
        			.setHeader("Access-Control-Max-Age", "3600")
        			// 允许的header参数
        			.setHeader("Access-Control-Allow-Headers", "*");
        			
        			// 如果是预检请求，则立即返回到前端 
        			PkuseRouter.match(PkuseHttpMethod.OPTIONS)
        				.free(r -> System.out.println("--------OPTIONS预检请求，不做处理"))
        				.back();
        		})
        		;
    }

}
