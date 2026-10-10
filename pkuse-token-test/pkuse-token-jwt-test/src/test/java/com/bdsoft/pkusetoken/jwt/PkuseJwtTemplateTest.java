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
package com.bdsoft.pkusetoken.jwt;

import com.bdsoft.pkusetoken.jwt.error.PkuseJwtErrorCode;
import com.bdsoft.pkusetoken.jwt.exception.PkuseJwtException;
import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * PkuseJwtTemplate 单元测试
 */
public class PkuseJwtTemplateTest {

	private static final String KEYT = "abcdefghijklmnopqrstuvwxyz";

	private final PkuseJwtTemplate template = new PkuseJwtTemplate();

	@Test
	public void createToken_shouldAllowCustomExtraData() {
		Map<String, Object> extraData = new HashMap<>();
		extraData.put("tenant", "t1");

		String token = template.createToken("login", 10001, "pc", 3600, extraData, KEYT);
		JSONObject payloads = template.getPayloads(token, "login", KEYT);

		Assertions.assertEquals("10001", payloads.getStr(PkuseJwtTemplate.LOGIN_ID));
		Assertions.assertEquals("t1", payloads.getStr("tenant"));
	}

	@Test
	public void createToken_shouldRejectReservedKeyInExtraData_simpleMode() {
		Map<String, Object> extraData = Collections.singletonMap(PkuseJwtTemplate.LOGIN_ID, "admin");

		PkuseJwtException e = Assertions.assertThrows(PkuseJwtException.class, () ->
				template.createToken("login", 10001, extraData, KEYT)
		);
		Assertions.assertEquals(PkuseJwtErrorCode.CODE_30207, e.getCode());
	}

	@Test
	public void createToken_shouldRejectReservedKeyInExtraData_fullMode() {
		Map<String, Object> extraData = Collections.singletonMap(PkuseJwtTemplate.EFF, -1L);

		PkuseJwtException e = Assertions.assertThrows(PkuseJwtException.class, () ->
				template.createToken("login", 10001, "pc", 3600, extraData, KEYT)
		);
		Assertions.assertEquals(PkuseJwtErrorCode.CODE_30207, e.getCode());
	}

	@Test
	public void createToken_shouldNotOverwriteLoginIdWhenExtraDataIsSafe() {
		String token = template.createToken("login", 10001, "pc", 3600, null, KEYT);
		JSONObject payloads = template.getPayloads(token, "login", KEYT);

		Assertions.assertEquals("10001", payloads.getStr(PkuseJwtTemplate.LOGIN_ID));
	}

}
