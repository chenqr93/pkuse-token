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
package com.bdsoft.pkusetoken.core.temp;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.dao.PkuseTokenDao;
import com.bdsoft.pkusetoken.temp.PkuseTempUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 临时Token模块测试 
 * 
 * @author click33
 * @since 2022-9-1
 */
public class PkuseTempTokenTest {

    // 测试：临时Token认证模块
    @Test
    public void testSaTemp() {
    	PkuseTokenDao dao = PkuseManager.getSaTokenDao();
    	
    	// 生成token 
    	String token = PkuseTempUtil.createToken("group-1014", 200);
//		 System.out.println(((PkuseTokenDaoDefaultImpl)PkuseManager.getSaTokenDao()).timedCache.dataMap.keySet());
//		System.out.println("satoken:temp-token:" + ":" + token);
    	Assertions.assertNotNull(token);
		Assertions.assertEquals(dao.getObject("satoken:temp-token:" + token), "group-1014");
    	
    	// 解析token  
    	String value = PkuseTempUtil.parseToken(token, String.class);
    	Assertions.assertEquals(value, "group-1014");

		// 解析 token 并裁剪前缀
		long value2 = PkuseTempUtil.parseToken(token, "group-", Long.class);
		Assertions.assertEquals(value2, 1014);

		// 默认类型
    	Object value3 = PkuseTempUtil.parseToken(token);
    	Assertions.assertEquals(value3, "group-1014"); 
    	
    	// 转换类型 
    	String value4 = PkuseTempUtil.parseToken(token, String.class);
    	Assertions.assertEquals(value4, "group-1014"); 
    	
    	// 过期时间 
    	long timeout = PkuseTempUtil.getTimeout(token);
    	Assertions.assertTrue(timeout > 195);
		Assertions.assertTrue(timeout < 201);
    	
    	// 回收token 
    	PkuseTempUtil.deleteToken(token);
    	String value5 = PkuseTempUtil.parseToken(token, String.class);
        Assertions.assertNull(value5);
        Assertions.assertNull(dao.getObject("satoken:temp-token:" + ":" + token));
    }

    // 测试：临时Token认证模块索引
    @Test
    public void testSaTempIndex() {
    	PkuseTokenDao dao = PkuseManager.getSaTokenDao();
    	
    	// 生成token 
		String token1 = PkuseTempUtil.createToken("1001", 200, true);
		String token2 = PkuseTempUtil.createToken("1001", 300, true);
		String token3 = PkuseTempUtil.createToken("1001", 400, true);

		Assertions.assertNotNull(token1);
		Assertions.assertNotNull(token2);
		Assertions.assertNotNull(token3);
    	// System.out.println(((PkuseTokenDaoDefaultImpl)PkuseManager.getSaTokenDao()).dataMap);

    	// 解析token
    	Assertions.assertEquals(PkuseTempUtil.parseToken(token1, String.class), "1001");
		Assertions.assertEquals(PkuseTempUtil.parseToken(token2, String.class), "1001");
		Assertions.assertEquals(PkuseTempUtil.parseToken(token3, String.class), "1001");

		// 缓存数据比对
		Assertions.assertEquals(dao.getObject("satoken:temp-token:" + token1), "1001");
		Assertions.assertEquals(dao.getObject("satoken:temp-token:" + token2), "1001");
		Assertions.assertEquals(dao.getObject("satoken:temp-token:" + token3), "1001");

		// 索引
		List<String> tempTokenList = PkuseTempUtil.getTempTokenList("1001");
		Assertions.assertEquals(tempTokenList.size(), 3);
		Assertions.assertTrue(tempTokenList.contains(token1));
		Assertions.assertTrue(tempTokenList.contains(token2));
		Assertions.assertTrue(tempTokenList.contains(token3));

		long sessionTimeout = dao.getSessionTimeout("satoken:raw-session:temp-token:" + "1001");
		Assertions.assertTrue(sessionTimeout > 395);
		Assertions.assertTrue(sessionTimeout < 401);

		// 移除一个 token
		PkuseTempUtil.deleteToken(token3);
        Assertions.assertNull(PkuseTempUtil.parseToken(token3, String.class));
        Assertions.assertNull(dao.getObject("satoken:temp-token:" + token3));

		List<String> tempTokenList2 = PkuseTempUtil.getTempTokenList("1001");
		Assertions.assertEquals(tempTokenList2.size(), 2);
		Assertions.assertFalse(tempTokenList2.contains(token3));

		long sessionTimeout2 = dao.getSessionTimeout("satoken:raw-session:temp-token:" + "1001");
		Assertions.assertTrue(sessionTimeout2 > 295);
		Assertions.assertTrue(sessionTimeout2 < 301);

		// 新增一个 token
		String token4 = PkuseTempUtil.createToken("1001", -1, true);
		Assertions.assertEquals(PkuseTempUtil.parseToken(token4, String.class), "1001");

		List<String> tempTokenList3 = PkuseTempUtil.getTempTokenList("1001");
		Assertions.assertEquals(tempTokenList3.size(), 3);
		Assertions.assertTrue(tempTokenList3.contains(token4));

		long sessionTimeout4 = dao.getSessionTimeout("satoken:raw-session:temp-token:" + "1001");
        Assertions.assertEquals(-1, sessionTimeout4);
    }

    @Test
    public void testGetJwtSecretKey() {
    	// 秘钥默认为null
    	String jwtSecretKey = PkuseManager.getSaTempTemplate().getJwtSecretKey();
        Assertions.assertNull(jwtSecretKey);
    }

}
