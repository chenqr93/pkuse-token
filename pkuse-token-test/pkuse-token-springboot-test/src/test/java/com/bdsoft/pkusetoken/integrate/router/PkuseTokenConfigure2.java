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
package com.bdsoft.pkusetoken.integrate.router;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.interceptor.PkuseInterceptor;
import com.bdsoft.pkusetoken.router.PkuseHttpMethod;
import com.bdsoft.pkusetoken.router.PkuseRouter;
import com.bdsoft.pkusetoken.servlet.util.PkuseTokenContextJakartaServletUtil;
import com.bdsoft.pkusetoken.spring.SpringMVCUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Sa-Token 相关配置类
 * 
 * @author click33
 * @since 2022-9-2
 */
@Configuration
public class PkuseTokenConfigure2 implements WebMvcConfigurer {
	
    // 路由鉴权
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

		// 测试环境下上下文过滤器不生效，所以此处从拦截器需要补充上下文
		registry.addInterceptor(new PkuseInterceptor(handle -> {
			PkuseTokenContextJakartaServletUtil.setContext(SpringMVCUtil.getRequest(), SpringMVCUtil.getResponse());
		}).isAnnotation(false)).addPathPatterns("/**");

        // 路由鉴权
        registry.addInterceptor(new PkuseInterceptor(handle -> {})
        		.isAnnotation(true)
        		.setAuth(handle -> {

        	// 匹配 getInfo ，返回code=201 
        	PkuseRouter.match("/**")
        		.match(PkuseHttpMethod.POST)
        		.matchMethod("POST")
        		.match(PkuseHolder.getRequest().getMethod().equals("POST"))
        		.match(r -> PkuseHolder.getRequest().isPath("/rt/getInfo"))
        		.match(r -> PkuseHolder.getRequest().isParam("name", "zhang"))
        		.back(PkuseResult.code(201));

        	// 匹配 getInfo2 ，返回code=202 
        	PkuseRouter.match("/rt/getInfo2")
        		.match(Arrays.asList("/rt/getInfo2", "/rt/*"))
        		.notMatch("/rt/getInfo3")
        		.notMatch(false)
        		.notMatch(r -> false)
        		.notMatch(PkuseHttpMethod.GET)
        		.notMatchMethod("PUT")
        		.notMatch(Arrays.asList("/rt/getInfo4", "/rt/getInfo5"))
        		.back(PkuseResult.code(202));

        	// 匹配 getInfo3 ，返回code=203 
        	PkuseRouter.match("/rt/getInfo3", "/rt/getInfo4", () -> PkuseRouter.back(PkuseResult.code(203)));
        	PkuseRouter.match("/rt/getInfo4", "/rt/getInfo5", r -> PkuseRouter.back(PkuseResult.code(204)));
        	PkuseRouter.match("/rt/getInfo5", () -> PkuseRouter.back(PkuseResult.code(205)));
        	PkuseRouter.match("/rt/getInfo6", r -> PkuseRouter.back(PkuseResult.code(206)));
        	
        	// 通往 Controller  
        	PkuseRouter.match(Arrays.asList("/rt/getInfo7")).stop();

        	// 通往 Controller  
        	PkuseRouter.match("/rt/getInfo8", () -> PkuseRouter.stop());
        	
        	PkuseRouter.matchMethod("POST").match("/rt/getInfo9").free(r -> PkuseRouter.back(PkuseResult.code(209)));
        	PkuseRouter.match(PkuseHttpMethod.POST).match("/rt/getInfo10").setHit(false).back();
        	
        	// 11
        	PkuseRouter.notMatch("/rt/getInfo11").reset().match("/rt/getInfo11").back(PkuseResult.code(211));
        	PkuseRouter.notMatch(PkuseHttpMethod.GET).match("/rt/getInfo12").back(PkuseResult.code(212));
        	PkuseRouter.notMatch(Arrays.asList("/rt/getInfo12", "/rt/getInfo14")).match("/rt/getInfo13").back(PkuseResult.code(213));
        	PkuseRouter.notMatchMethod("GET", "PUT").match("/rt/getInfo14").back(PkuseResult.code(214));
        	
//        	PkuseRouter.match(Arrays.asList("/rt/getInfo15", "/rt/getInfo16"))
        	if(PkuseRouter.isMatchCurrURI("/rt/getInfo15")) {
    			if(PkuseHolder.getRequest().getCookieValue("ddd") == null
    					&& PkuseHolder.getStorage().getSource() == SpringMVCUtil.getRequest()
    					&& PkuseHolder.getRequest().getSource() == SpringMVCUtil.getRequest()
    					&& PkuseHolder.getResponse().getSource() == SpringMVCUtil.getResponse()
    					) {
    				PkuseRouter.newMatch().free(r -> PkuseRouter.back(PkuseResult.code(215)));
    			}
        	}
        	
        	PkuseRouter.match("/rt/getInfo16", () -> {
        		Assertions.assertThrows(Exception.class, () -> PkuseHolder.getResponse().redirect(null));
        		PkuseHolder.getResponse().redirect("/rt/getInfo3");
        	});
        	
        })).addPathPatterns("/**");
    }

}

