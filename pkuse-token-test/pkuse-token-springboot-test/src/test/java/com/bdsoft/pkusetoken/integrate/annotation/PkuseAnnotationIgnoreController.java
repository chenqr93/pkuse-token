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

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bdsoft.pkusetoken.annotation.PkuseCheckLogin;
import com.bdsoft.pkusetoken.annotation.PkuseIgnore;
import com.bdsoft.pkusetoken.util.PkuseResult;

/**
 * 测试注解用的Controller 
 * 
 * @author click33
 * @since 2022-9-2
 */
@PkuseCheckLogin
@RestController
@RequestMapping("/ig/")
public class PkuseAnnotationIgnoreController {

	// 需要登录后访问  
	@RequestMapping("show1")
	public PkuseResult show1() {
		return PkuseResult.ok();
	}

	// 不登录也可访问  
	@PkuseIgnore
	@RequestMapping("show2")
	public PkuseResult show2() {
		return PkuseResult.ok();
	}

}
