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

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bdsoft.pkusetoken.httpauth.basic.PkuseHttpBasicUtil;
import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;

/**
 * 其它测试 
 * 
 * @author click33
 *
 */
@RestController
@RequestMapping("/more/")
public class MoreController {

	// 一些基本的测试 
    @RequestMapping("getInfo")
    public PkuseResult getInfo() {
    	PkuseRequest req = PkuseHolder.getRequest();
    	boolean flag = 
    			PkuseFoxUtil.equals(req.getParam("name"), "zhang")
    			&& PkuseFoxUtil.equals(req.getParam("name2", "li"), "li")
    			&& PkuseFoxUtil.equals(req.getParamNotNull("name"), "zhang")
    			&& req.isParam("name", "zhang")
    			&& req.isPath("/more/getInfo")
    			&& req.hasParam("name")
    			&& PkuseFoxUtil.equals(req.getHeader("div"), "val")
    			&& PkuseFoxUtil.equals(req.getHeader("div", "zhang"), "val")
    			&& PkuseFoxUtil.equals(req.getHeader("div2", "zhang"), "zhang")
    			;

    	PkuseHolder.getResponse().setServer("sa-server");
    	return PkuseResult.data(flag);
    }

	// Http Basic 认证 
    @RequestMapping("basicAuth")
    public PkuseResult basicAuth() {
    	PkuseHttpBasicUtil.check("sa:123456");
    	return PkuseResult.ok();
    }
    
}
