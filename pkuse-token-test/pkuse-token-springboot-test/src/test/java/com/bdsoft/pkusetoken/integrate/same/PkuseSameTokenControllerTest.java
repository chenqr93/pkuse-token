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
package com.bdsoft.pkusetoken.integrate.same;

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
import com.bdsoft.pkusetoken.exception.SameTokenInvalidException;
import com.bdsoft.pkusetoken.integrate.StartUpApplication;
import com.bdsoft.pkusetoken.same.PkuseSameUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;

/**
 * same-token Controller 测试 
 * 
 * @author click33
 *
 */
@SpringBootTest(classes = StartUpApplication.class)
public class PkuseSameTokenControllerTest {

	@Autowired
	private WebApplicationContext wac;
	 
	private MockMvc mvc;
	
	// 开始 
	@BeforeEach
    public void before() {
		mvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }
	
	// 获取信息 
	@Test
	public void testGetInfo() {
		String token = PkuseSameUtil.getToken();
		// 加token，能调通 
		PkuseResult res = request("/same/getInfo", token);
		Assertions.assertEquals(res.getCode(), 200);
		// 不加token，不能调通 
		PkuseResult res2 = request("/same/getInfo", "xxx");
		Assertions.assertEquals(res2.getCode(), 902);

		// 获取信息2  
		token = PkuseSameUtil.getTokenNh();
		// 加token，能调通 
		PkuseResult res3 = request("/same/getInfo2", token);
		Assertions.assertEquals(res3.getCode(), 200);
		// 不加token，不能调通 
		PkuseResult res4 = request("/same/getInfo2", "xxx");
		Assertions.assertEquals(res4.getCode(), 902);
	}

	// 基础测试 
	@Test
	public void testApi() {
		String token = PkuseSameUtil.getToken();
		
		// 刷新一下，会有变化 
		PkuseSameUtil.refreshToken();
		String token2 = PkuseSameUtil.getToken();
		Assertions.assertNotEquals(token, token2);
		
		// 旧token，变为次级token
		String pastToken = PkuseSameUtil.getPastTokenNh();
		Assertions.assertEquals(token, pastToken);
		
		// dao中应该有值 
		String daoToken = PkuseManager.getSaTokenDao().get("satoken:var:same-token");
		String daoToken2 = PkuseManager.getSaTokenDao().get("satoken:var:past-same-token");
		Assertions.assertEquals(token2, daoToken);
		Assertions.assertEquals(token, daoToken2);
		
		// 新旧都有效 
		Assertions.assertTrue(PkuseSameUtil.isValid(token));
		Assertions.assertTrue(PkuseSameUtil.isValid(token2));
		
		// 空的不行 
		Assertions.assertFalse(PkuseSameUtil.isValid(null));
		Assertions.assertFalse(PkuseSameUtil.isValid(""));
		
		// 不抛出异常 
		Assertions.assertDoesNotThrow(() -> PkuseSameUtil.checkToken(token));
		Assertions.assertDoesNotThrow(() -> PkuseSameUtil.checkToken(token2));
		
		// 抛出异常
		Assertions.assertThrows(SameTokenInvalidException.class, () -> PkuseSameUtil.checkToken(null));
		Assertions.assertThrows(SameTokenInvalidException.class, () -> PkuseSameUtil.checkToken(""));
		Assertions.assertThrows(SameTokenInvalidException.class, () -> PkuseSameUtil.checkToken("aaa"));
	}

	
	
    // 封装请求 
    private PkuseResult request(String path, String sameToken) {
    	try {
    		// 发请求 
        	MvcResult mvcResult = mvc.perform(
        				MockMvcRequestBuilders.post(path)
    					.contentType(MediaType.APPLICATION_PROBLEM_JSON)
    					.accept(MediaType.APPLICATION_PROBLEM_JSON)
    					.header(PkuseSameUtil.SAME_TOKEN, sameToken)
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
