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
package com.bdsoft.pkusetoken.httpauth.basic;

import com.bdsoft.pkusetoken.json.PkuseJsonType;

import com.bdsoft.pkusetoken.exception.PkuseTokenException;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;

/**
 * Sa-Token Http Basic 账号
 *
 * @author click33
 * @since 1.41.0
 */
public class PkuseHttpBasicAccount implements PkuseJsonType {

	/**
	 * 账号
	 */
	private String username;

	/**
	 * 密码
	 */
	private String password;

	/**
	 * 构造函数
	 * @param username 账号
	 * @param password 密码
	 */
	public PkuseHttpBasicAccount(String username, String password) {
		this.username = username;
		this.password = password;
	}

	/**
	 * 构造函数
	 * @param usernameAndPassword 账号和密码，冒号隔开
	 */
	public PkuseHttpBasicAccount(String usernameAndPassword) {
		if(PkuseFoxUtil.isEmpty(usernameAndPassword)) {
			throw new PkuseTokenException("UsernameAndPassword 不能为空");
		}
		String[] arr = usernameAndPassword.split(":");
		if(arr.length != 2) {
			throw new PkuseTokenException("UsernameAndPassword 格式错误，正确格式为：username:password");
		}
		this.username = arr[0];
		this.password = arr[1];
	}

	/**
	 * 获取 账号
	 *
	 * @return username 账号
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * 设置 账号
	 *
	 * @param username 账号
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * 获取 密码
	 *
	 * @return password 密码
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * 设置 密码
	 *
	 * @param password 密码
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String toString() {
		return "PkuseHttpBasicAccount{" +
				"username='" + username + '\'' +
				", password='" + password + '\'' +
				'}';
	}

}
