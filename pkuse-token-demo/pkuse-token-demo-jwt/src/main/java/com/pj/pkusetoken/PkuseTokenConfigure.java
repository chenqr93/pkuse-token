package com.pj.pkusetoken;

import com.bdsoft.pkusetoken.interceptor.PkuseInterceptor;
import com.bdsoft.pkusetoken.jwt.StpLogicJwtForSimple;
import com.bdsoft.pkusetoken.stp.StpLogic;
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
     * Sa-Token 整合 jwt 
     */
	@Bean
    public StpLogic getStpLogicJwt() {
		return new StpLogicJwtForSimple();
//		return new StpLogicJwtForMixin();
//		return new StpLogicJwtForStateless();
    }
    
}
