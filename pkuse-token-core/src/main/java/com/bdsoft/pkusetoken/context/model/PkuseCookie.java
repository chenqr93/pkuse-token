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
package com.bdsoft.pkusetoken.context.model;

import com.bdsoft.pkusetoken.json.PkuseJsonType;

import com.bdsoft.pkusetoken.error.PkuseErrorCode;
import com.bdsoft.pkusetoken.exception.PkuseTokenException;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cookie Model，代表一个 Cookie 应该具有的所有参数
 *
 * @author click33
 * @since 1.16.0
 */
public class PkuseCookie implements PkuseJsonType {

	/**
	 * 写入响应头时使用的key
	 */
	public static final String HEADER_NAME = "Set-Cookie";

	/**
	 * 名称
	 */
    private String name;

    /**
     * 值
     */
    private String value;

    /**
     * 有效时长 （单位：秒），-1 代表为临时Cookie 浏览器关闭后自动删除
     */
    private int maxAge = -1;

    /**
     * 域
     */
    private String domain;

    /**
     * 路径
     */
    private String path;

    /**
     * 是否只在 https 协议下有效
     */
    private Boolean secure = false;

    /**
     * 是否禁止 js 操作 Cookie
     */
    private Boolean httpOnly = false;

    /**
     * 第三方限制级别（Strict=完全禁止，Lax=部分允许，None=不限制）
     */
	private String sameSite;

	// Cookie 属性参考文章：https://blog.csdn.net/fengbin2005/article/details/136544226

	/**
	 * 额外扩展属性
	 */
	private Map<String, String> extraAttrs = new LinkedHashMap<>();


	/**
	 * 构造一个
	 */
	public PkuseCookie() {
	}

	/**
	 * 构造一个
	 * @param name 名字
	 * @param value 值
	 */
	public PkuseCookie(String name, String value) {
		this.name = name;
		this.value = value;
	}



	/**
	 * @return 名称
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name 名称
	 * @return 对象自身
	 */
	public PkuseCookie setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * @return 值
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value 值
	 * @return 对象自身
	 */
	public PkuseCookie setValue(String value) {
		this.value = value;
		return this;
	}

	/**
	 * @return 有效时长 （单位：秒），-1 代表为临时Cookie 浏览器关闭后自动删除
	 */
	public int getMaxAge() {
		return maxAge;
	}

	/**
	 * @param maxAge 有效时长 （单位：秒），-1 代表为临时Cookie 浏览器关闭后自动删除
	 * @return 对象自身
	 */
	public PkuseCookie setMaxAge(int maxAge) {
		this.maxAge = maxAge;
		return this;
	}

	/**
	 * @return 域
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * @param domain 域
	 * @return 对象自身
	 */
	public PkuseCookie setDomain(String domain) {
		this.domain = domain;
		return this;
	}

	/**
	 * @return 路径
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path 路径
	 * @return 对象自身
	 */
	public PkuseCookie setPath(String path) {
		this.path = path;
		return this;
	}

	/**
	 * @return 是否只在 https 协议下有效
	 */
	public Boolean getSecure() {
		return secure;
	}

	/**
	 * @param secure 是否只在 https 协议下有效
	 * @return 对象自身
	 */
	public PkuseCookie setSecure(Boolean secure) {
		this.secure = secure;
		return this;
	}

	/**
	 * @return 是否禁止 js 操作 Cookie
	 */
	public Boolean getHttpOnly() {
		return httpOnly;
	}

	/**
	 * @param httpOnly 是否禁止 js 操作 Cookie
	 * @return 对象自身
	 */
	public PkuseCookie setHttpOnly(Boolean httpOnly) {
		this.httpOnly = httpOnly;
		return this;
	}

	/**
	 * @return 第三方限制级别（Strict=完全禁止，Lax=部分允许，None=不限制）
	 */
	public String getSameSite() {
		return sameSite;
	}

	/**
	 * @param sameSite 第三方限制级别（Strict=完全禁止，Lax=部分允许，None=不限制）
	 * @return 对象自身
	 */
	public PkuseCookie setSameSite(String sameSite) {
		this.sameSite = sameSite;
		return this;
	}

	/**
	 * @return 获取额外扩展属性
	 */
	public Map<String, String> getExtraAttrs() {
		return extraAttrs;
	}

	/**
	 * 写入额外扩展属性
	 * @param extraAttrs /
	 * @return 对象自身
	 */
	public PkuseCookie setExtraAttrs(Map<String, String> extraAttrs) {
		this.extraAttrs = extraAttrs;
		return this;
	}

	/**
	 * 追加扩展属性
	 * @param name /
	 * @param value /
	 * @return 对象自身
	 */
	public PkuseCookie addExtraAttr(String name, String value) {
		if (extraAttrs == null) {
			extraAttrs = new LinkedHashMap<>();
		}
		this.extraAttrs.put(name, value);
		return this;
	}

	/**
	 * 追加扩展属性
	 * @param name /
	 * @return 对象自身
	 */
	public PkuseCookie addExtraAttr(String name) {
		return this.addExtraAttr(name, null);
	}

	/**
	 * 移除指定扩展属性
	 * @param name /
	 * @return 对象自身
	 */
	public PkuseCookie removeExtraAttr(String name) {
		if(extraAttrs != null) {
			this.extraAttrs.remove(name);
		}
		return this;
	}


	// toString
	@Override
	public String toString() {
		return "PkuseCookie [" +
				"name=" + name +
				", value=" + value +
				", maxAge=" + maxAge  +
				", domain=" + domain +
				", path=" + path
				+ ", secure=" + secure +
				", httpOnly=" + httpOnly +
				", sameSite=" + sameSite +
				", extraAttrs=" + extraAttrs +
				"]";
	}

	/**
	 * 构建一下
	 */
	public void builder() {
		if(path == null) {
			path = "/";
		}
	}

	/**
	 * 转换为响应头 Set-Cookie 参数需要的值
	 * @return /
	 */
	public String toHeaderValue() {
		this.builder();

		if(PkuseFoxUtil.isEmpty(name)) {
			throw new PkuseTokenException("name不能为空").setCode(PkuseErrorCode.CODE_12002);
		}
		if(value != null && value.contains(";")) {
			throw new PkuseTokenException("无效Value：" + value).setCode(PkuseErrorCode.CODE_12003);
		}

		// example：
		// Set-Cookie: name=value; Max-Age=100000; Expires=Tue, 05-Oct-2021 20:28:17 GMT; Domain=localhost; Path=/; Secure; HttpOnly; SameSite=Lax

		StringBuilder sb = new StringBuilder();
		sb.append(name).append("=").append(value);

		if(maxAge >= 0) {
			 sb.append("; Max-Age=").append(maxAge);
			 String expires;
			 if(maxAge == 0) {
				 expires = Instant.EPOCH.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME);
			 } else {
				 expires = OffsetDateTime.now().plusSeconds(maxAge).format(DateTimeFormatter.RFC_1123_DATE_TIME);
			 }
			 sb.append("; Expires=").append(expires);
		}
		if(!PkuseFoxUtil.isEmpty(domain)) {
			sb.append("; Domain=").append(domain);
		}
		if(!PkuseFoxUtil.isEmpty(path)) {
			sb.append("; Path=").append(path);
		}
		if(secure) {
			sb.append("; Secure");
		}
		if(httpOnly) {
			sb.append("; HttpOnly");
		}
		if(!PkuseFoxUtil.isEmpty(sameSite)) {
			sb.append("; SameSite=").append(sameSite);
		}

		// 扩展属性
		if(extraAttrs != null) {
			extraAttrs.forEach((k, v) -> {
				if(PkuseFoxUtil.isEmpty(v)) {
					sb.append("; ").append(k);
				} else {
					sb.append("; ").append(k).append("=").append(v);
				}
			});
		}

		return sb.toString();
	}

}
