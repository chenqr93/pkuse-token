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

import java.util.Arrays;
import java.util.Map;

import jakarta.servlet.http.Cookie;

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
import com.bdsoft.pkusetoken.router.PkuseRouter;
import com.bdsoft.pkusetoken.router.PkuseRouterStaff;
import com.bdsoft.pkusetoken.servlet.model.PkuseRequestForServlet;
import com.bdsoft.pkusetoken.spring.PkuseTokenContextForSpringInJakartaServlet;
import com.bdsoft.pkusetoken.spring.SpringMVCUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;


/**
 * C Controller 测试 
 * 
 * @author click33
 *
 */
@SpringBootTest(classes = StartUpApplication.class)
public class RouterControllerTest {

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
		// 是否命中 
    	PkuseRouterStaff staff = PkuseRouter.match(false);
    	Assertions.assertFalse(staff.isHit());

    	// 重置 
    	staff.reset();
    	Assertions.assertTrue(staff.isHit());
    	
    	// lambda 形式 
    	PkuseRouterStaff staff2 = PkuseRouter.match(r -> false);
    	Assertions.assertFalse(staff2.isHit());
    	
    	// 匹配 
    	Assertions.assertTrue(PkuseRouter.isMatch("/user/**", "/user/add"));
    	Assertions.assertTrue(PkuseRouter.isMatch(new String[] {"/user/**", "/art/**", "/goods/**"}, "/art/delete"));
    	Assertions.assertTrue(PkuseRouter.isMatch(Arrays.asList("/user/**", "/art/**", "/goods/**"), "/art/delete"));
    	Assertions.assertTrue(PkuseRouter.isMatch(new String[] {"POST", "GET", "PUT"},  "GET"));
    	
    	// 不匹配的 
    	Assertions.assertTrue(PkuseRouter.notMatch(false).isHit());
    	Assertions.assertTrue(PkuseRouter.notMatch(r -> false).isHit());
	}
	
	// 各种路由测试 
	@Test
	public void testRouter() {
		// getInfo 
		PkuseResult res = request("/rt/getInfo?name=zhang");
		Assertions.assertEquals(res.getCode(), 201);
		
		// getInfo2 
		PkuseResult res2 = request("/rt/getInfo2");
		Assertions.assertEquals(res2.getCode(), 202);

		// getInfo3 
		PkuseResult res3 = request("/rt/getInfo3");
		Assertions.assertEquals(res3.getCode(), 203);

		// getInfo4 
		PkuseResult res4 = request("/rt/getInfo4");
		Assertions.assertEquals(res4.getCode(), 204);
		
		// getInfo5 
		PkuseResult res5 = request("/rt/getInfo5");
		Assertions.assertEquals(res5.getCode(), 205);
		
		// getInfo6 
		PkuseResult res6 = request("/rt/getInfo6");
		Assertions.assertEquals(res6.getCode(), 206);
		
		// getInfo7 
		PkuseResult res7 = request("/rt/getInfo7");
		Assertions.assertEquals(res7.getCode(), 200);
		
		// getInfo8 
		PkuseResult res8 = request("/rt/getInfo8");
		Assertions.assertEquals(res8.getCode(), 200);
		
		// getInfo9 
		PkuseResult res9 = request("/rt/getInfo9");
		Assertions.assertEquals(res9.getCode(), 209);
		
		// getInfo10 
		PkuseResult res10 = request("/rt/getInfo10");
		Assertions.assertEquals(res10.getCode(), 200);
		
		// getInfo11 
		PkuseResult res11 = request("/rt/getInfo11");
		Assertions.assertEquals(res11.getCode(), 211);
		
		// getInfo12
		PkuseResult res12 = request("/rt/getInfo12");
		Assertions.assertEquals(res12.getCode(), 212);
		
		// getInfo13
		PkuseResult res13 = request("/rt/getInfo13");
		Assertions.assertEquals(res13.getCode(), 213);
		
		// getInfo14
		PkuseResult res14 = request("/rt/getInfo14");
		Assertions.assertEquals(res14.getCode(), 214);
		
		// getInfo15
		PkuseResult res15 = request("/rt/getInfo15");
		Assertions.assertEquals(res15.getCode(), 215);
		
	}

	// 测试 getUrl() 
	@Test
	public void testGetUrl() {
		// getInfo_101 
		PkuseResult res = request("/rt/getInfo_101");
		Assertions.assertTrue(res.getData().toString().endsWith("/rt/getInfo_101"));
		
		// getInfo_101，不包括后面的参数 
		PkuseResult res2 = request("/rt/getInfo_101?id=1");
		Assertions.assertTrue(res2.getData().toString().endsWith("/rt/getInfo_101"));
		
		// 自定义当前域名 
		PkuseManager.getConfig().setCurrDomain("http://xxx.com");
		PkuseResult res3 = request("/rt/getInfo_101?id=1");
		Assertions.assertEquals(res3.getData().toString(), "http://xxx.com/rt/getInfo_101");
		PkuseManager.getConfig().setCurrDomain(null);
	}

	// 测试读取Cookie 
	@Test
	public void testGetCookie() throws Exception {
		MvcResult mvcResult = mvc.perform(
				MockMvcRequestBuilders.post("/rt/getInfo_102")
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.accept(MediaType.APPLICATION_PROBLEM_JSON)
				.cookie(new Cookie("x-token", "token-111"))
			)
			.andExpect(MockMvcResultMatchers.status().is(200))
			.andReturn();
		
		// 转 Map 
		String content = mvcResult.getResponse().getContentAsString();
		Map<String, Object> map = PkuseManager.getSaJsonTemplate().jsonToMap(content);
		
		// 转 PkuseResult 对象 
		PkuseResult res = new PkuseResult().setMap(map);
		Assertions.assertEquals(res.getData(), "token-111");
	}
	
	// 测试重定向 
	@Test
	public void testRedirect() throws Exception {
		MvcResult mvcResult = mvc.perform(
				MockMvcRequestBuilders.post("/rt/getInfo16")
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.accept(MediaType.APPLICATION_PROBLEM_JSON)
			)
			.andExpect(MockMvcResultMatchers.status().is(302))
			.andReturn();
	
		Assertions.assertEquals(mvcResult.getResponse().getHeader("Location"), "/rt/getInfo3");
	}

	// 空接口 
	@Test
	public void testGetInfo200() {
//		PkuseResult res = request("/rt/getInfo_200");
//		Assertions.assertEquals(res.getCode(), 200);
//		PkuseResult res1 = request("/rt/getInfo_201");
//		Assertions.assertEquals(res1.getCode(), 201);
//		PkuseResult res2 = request("/rt/getInfo_202");
//		Assertions.assertEquals(res2.getCode(), 401);
		
		// 登录拿到Token 
    	PkuseResult resLogin = request("/rt/login?id=10001");
    	String satoken = resLogin.get("token", String.class);
		PkuseResult res3 = request("/rt/getInfo_202?satoken=" + satoken);
		Assertions.assertEquals(res3.getCode(), 200);
	}

	// 测试转发 
	@Test
	public void testForward() {
		PkuseResult res = request("/rt/getInfo_103");
		Assertions.assertEquals(res.getCode(), 200);
	}
	
    // 封装请求 
    private PkuseResult request(String path) {
    	try {
    		// 发请求 
        	MvcResult mvcResult = mvc.perform(
        				MockMvcRequestBuilders.post(path)
    					.contentType(MediaType.APPLICATION_PROBLEM_JSON)
    					.accept(MediaType.APPLICATION_PROBLEM_JSON)
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
