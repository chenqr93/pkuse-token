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
package com.bdsoft.pkusetoken.core.session;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.dao.PkuseTokenDao;
import com.bdsoft.pkusetoken.session.PkuseSession;
import com.bdsoft.pkusetoken.session.PkuseSessionCustomUtil;

/**
 * PkuseSession 测试 
 * 
 * @author click33
 * @since 2022-2-9 
 */
public class PkuseSessionCustomUtilTest {

    // 测试自定义Session 
    @Test
    public void testCustomSession() {
    	PkuseTokenDao dao = PkuseManager.getSaTokenDao();
    	
    	// 刚开始不存在 
    	Assertions.assertFalse(PkuseSessionCustomUtil.isExists("art-1"));
    	PkuseSession session = dao.getSession("satoken:custom:session:" + "art-1");
    	Assertions.assertNull(session);
    	
    	// 调用一下
    	PkuseSessionCustomUtil.getSessionById("art-1");
    	PkuseSessionCustomUtil.getSessionById("art-1", false);
    	
    	// 就存在了 
    	Assertions.assertTrue(PkuseSessionCustomUtil.isExists("art-1"));
    	PkuseSession session2 = dao.getSession("satoken:custom:session:" + "art-1");
    	Assertions.assertNotNull(session2);
    	
    	// 给删除掉 
    	PkuseSessionCustomUtil.deleteSessionById("art-1");
    	
    	// 就又不存在了 
    	Assertions.assertFalse(PkuseSessionCustomUtil.isExists("art-1"));
    	PkuseSession session3 = dao.getSession("satoken:custom:session:" + "art-1");
    	Assertions.assertNull(session3);
    	
    	// 调用了也不会存在 
    	PkuseSessionCustomUtil.getSessionById("art-4", false);
    	PkuseSession session4 = dao.getSession("satoken:custom:session:" + "art-2");
    	Assertions.assertNull(session4);
    }
    
}
