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
package com.bdsoft.pkusetoken.integrate.more;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.integrate.StartUpApplication;
import com.bdsoft.pkusetoken.servlet.model.PkuseRequestForServlet;
import com.bdsoft.pkusetoken.spring.PkuseTokenContextForSpringInJakartaServlet;
import com.bdsoft.pkusetoken.spring.SpringMVCUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;


/**
 * 其它测试 
 * 
 * @author click33
 *
 */
@SpringBootTest(classes = StartUpApplication.class)
public class MoreControllerTest {

	@Autowired
	private WebApplicationContext wac;
	 
	private MockMvc mvc;

	// 开始 
	@BeforeEach
    public void before() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
		
		// 在单元测试时，通过 request.getServletPath() 获取到的请求路径为空，导致路由拦截不正确 
		// 虽然不知道为什么会这样，但是暂时可以通过以下方式来解决 
		PkuseManager.setSaTokenContext(new PkuseTokenContextForSpringInJakartaServlet() {
			@Override
			public PkuseRequest getRequest() {
				return new PkuseRequestForServlet(SpringMVCUtil.getRequest()) {
					@Override
					public String getRequestPath() {
						return request.getRequestURI();
					}
				};
			}
		});
		
    }

	// 基础API测试 
	@Test
	public void testApi() {
		PkuseResult res = request("/more/getInfo?name=zhang");
		Assertions.assertEquals(res.getData(), true);
	}

	// Http Basic 认证 
	@Test
	public void testBasic() throws Exception {
		
		// ---------------- 认证不通过
		MvcResult mvcResult = mvc.perform(
				MockMvcRequestBuilders.post("/more/basicAuth")
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.accept(MediaType.APPLICATION_PROBLEM_JSON)
			)
			.andExpect(MockMvcResultMatchers.status().is(401))
			.andReturn();
	
		// 转 Map 
		String content = mvcResult.getResponse().getContentAsString();
		Map<String, Object> map = PkuseManager.getSaJsonTemplate().jsonToMap(content);
		// 转 PkuseResult 对象 
		PkuseResult res = new PkuseResult().setMap(map);
		Assertions.assertEquals(res.getCode(), 903);
		// 会有一个特殊响应头
		String header = mvcResult.getResponse().getHeader("WWW-Authenticate");
		Assertions.assertEquals(header, "Basic Realm=Sa-Token");
		
		
		// ---------------- 认证通过
    	MvcResult mvcResult2 = mvc.perform(
    				MockMvcRequestBuilders.post("/more/basicAuth")
					.contentType(MediaType.APPLICATION_PROBLEM_JSON)
					.accept(MediaType.APPLICATION_PROBLEM_JSON)
					.header("Authorization", "Basic c2E6MTIzNDU2")
    			)
    			.andExpect(MockMvcResultMatchers.status().isOk())
    			.andReturn();
    	
		// 转 Map 
		String content2 = mvcResult2.getResponse().getContentAsString();
		Map<String, Object> map2 = PkuseManager.getSaJsonTemplate().jsonToMap(content2);
		// 转 PkuseResult 对象 
		PkuseResult res2 = new PkuseResult().setMap(map2);
		Assertions.assertEquals(res2.getCode(), 200);
	}
	

    // 封装请求 
    private PkuseResult request(String path) {
    	try {
    		// 发请求 
        	MvcResult mvcResult = mvc.perform(
        				MockMvcRequestBuilders.post(path)
    					.contentType(MediaType.APPLICATION_PROBLEM_JSON)
    					.accept(MediaType.APPLICATION_PROBLEM_JSON)
    					.header("div", "val")
        			)
        			.andExpect(MockMvcResultMatchers.status().isOk())
        			.andReturn();
        	
    		// 转 Map 
    		String content = mvcResult.getResponse().getContentAsString();
    		Map<String, Object> map = PkuseManager.getSaJsonTemplate().jsonToMap(content);
    		
    		// 转 PkuseResult 对象 
    		return new PkuseResult().setMap(map);
    		
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
    
}
