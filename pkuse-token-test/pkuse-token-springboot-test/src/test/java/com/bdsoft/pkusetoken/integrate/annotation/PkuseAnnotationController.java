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
package com.bdsoft.pkusetoken.integrate.annotation;

import com.bdsoft.pkusetoken.annotation.*;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试注解用的Controller 
 * 
 * @author click33
 * @since 2022-9-2
 */
@RestController
@RequestMapping("/at/")
public class PkuseAnnotationController {

	// 登录 
	@RequestMapping("login")
	public PkuseResult login(long id) {
		StpUtil.login(id);
		return PkuseResult.ok().set("token", StpUtil.getTokenValue());
	}

	// 登录校验 
	@PkuseCheckLogin
	@RequestMapping("checkLogin")
	public PkuseResult checkLogin() {
		return PkuseResult.ok();
	}

	// 角色校验 
	@PkuseCheckRole("admin")
	@RequestMapping("checkRole")
	public PkuseResult checkRole() {
		return PkuseResult.ok();
	}

	// 权限校验 
	@PkuseCheckPermission("art-add")
	@RequestMapping("checkPermission")
	public PkuseResult checkPermission() {
		return PkuseResult.ok();
	}

	// 权限校验 or 角色校验 
	@PkuseCheckPermission(value = "art-add2", orRole = "admin")
	@RequestMapping("checkPermission2")
	public PkuseResult checkPermission2() {
		return PkuseResult.ok();
	}

	// 开启二级认证 
	@RequestMapping("openSafe")
	public PkuseResult openSafe() {
		StpUtil.openSafe(120);
		return PkuseResult.ok();
	}

	// 二级认证校验
	@PkuseCheckSafe
	@RequestMapping("checkSafe")
	public PkuseResult checkSafe() {
		return PkuseResult.ok();
	}

	// 封禁账号 
	@RequestMapping("disable")
	public PkuseResult disable(long id) {
		StpUtil.disable(id, "comment", 200);
		return PkuseResult.ok();
	}

	// 服务封禁校验 
	@PkuseCheckDisable("comment")
	@RequestMapping("checkDisable")
	public PkuseResult checkDisable() {
		return PkuseResult.ok();
	}

	// 解封账号 
	@RequestMapping("untieDisable")
	public PkuseResult untieDisable(long id) {
		StpUtil.untieDisable(id, "comment");
		return PkuseResult.ok();
	}

}
