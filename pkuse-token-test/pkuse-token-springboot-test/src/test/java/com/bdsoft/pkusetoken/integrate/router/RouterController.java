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

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;

/**
 * 路由鉴权测试 
 * 
 * @author click33
 *
 */
@RestController
@RequestMapping("/rt/")
public class RouterController {

    @RequestMapping("getInfo")
    public PkuseResult getInfo() {
    	return PkuseResult.ok();
    }

    @RequestMapping("getInfo*")
    public PkuseResult getInfo2() {
    	return PkuseResult.ok();
    }

    // 读url 
    @RequestMapping("getInfo_101")
    public PkuseResult getInfo_101() {
    	return PkuseResult.data(PkuseHolder.getRequest().getUrl());
    }

    // 读Cookie 
    @RequestMapping("getInfo_102")
    public PkuseResult getInfo_102() {
    	return PkuseResult.data(PkuseHolder.getRequest().getCookieValue("x-token"));
    }

    // 测试转发 
    @RequestMapping("getInfo_103")
    public PkuseResult getInfo_103() {
    	PkuseHolder.getRequest().forward("/rt/getInfo_102");
    	return PkuseResult.ok();
    }

    // 空接口 
    @RequestMapping("getInfo_200")
    public PkuseResult getInfo_200() {
    	return PkuseResult.ok();
    }
    @RequestMapping("getInfo_201")
    public PkuseResult getInfo_201() {
    	return PkuseResult.ok();
    }
    @RequestMapping("getInfo_202")
    public PkuseResult getInfo_202() {
    	return PkuseResult.ok();
    }
	@RequestMapping("login")
	public PkuseResult login(long id) {
		StpUtil.login(id);
		return PkuseResult.ok().set("token", StpUtil.getTokenValue());
	}

}
