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
package com.bdsoft.pkusetoken.oauth2.data.model.loader;

import com.bdsoft.pkusetoken.json.PkuseJsonType;

import com.bdsoft.pkusetoken.oauth2.strategy.PkuseOAuth2Strategy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Client 应用信息 Model
 *
 * @author click33
 * @since 1.23.0
 */
public class PkuseClientModel implements PkuseJsonType, Serializable {

	private static final long serialVersionUID = -6541180061782004705L;

	/**
	 * 应用id 
	 */
	public String clientId;
	
	/**
	 * 应用秘钥 
	 */
	public String clientSecret;

	/**
	 * 应用签约的所有权限
	 */
	public List<String> contractScopes = new ArrayList<>();
	
	/**
	 * 应用允许授权的所有 redirect_uri
	 */
	public List<String> allowRedirectUris = new ArrayList<>();

	/**
	 * 应用允许的所有 grant_type
	 */
	public List<String> allowGrantTypes = new ArrayList<>();

	/**
	 * 主体id
	 */
	public String subjectId;

	/** 此应用 Access-Token 保存的时间(单位秒)  [默认取全局配置] */
	public long accessTokenTimeout;

	/** 此应用 Refresh-Token 保存的时间(单位秒) [默认取全局配置] */
	public long refreshTokenTimeout;

	/** 此应用 Client-Token 保存的时间(单位秒) [默认取全局配置] */
	public long clientTokenTimeout;

	/** 此应用单个用户最多同时存在的 Access-Token 数量 */
	public int maxAccessTokenCount;

	/** 此应用单个用户最多同时存在的 Refresh-Token 数量 */
	public int maxRefreshTokenCount;

	/** 此应用最多同时存在的 Client-Token 数量 */
	public int maxClientTokenCount;

	/** 此应用 是否在每次 Refresh-Token 刷新 Access-Token 时，产生一个新的 Refresh-Token [默认取全局配置] */
	public Boolean isNewRefresh;

	/** 是否允许此应用自动确认授权（高危配置，禁止向不被信任的第三方开启此选项） */
	public Boolean isAutoConfirm = false;

	
	public PkuseClientModel() {
		PkuseOAuth2Strategy.instance.setSaClientModelDefaultFields.run(this);
	}
	public PkuseClientModel(String clientId, String clientSecret, List<String> contractScopes, List<String> allowRedirectUris) {
		this();
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.contractScopes = contractScopes;
		this.allowRedirectUris = allowRedirectUris;
	}


	// 追加方法

	/**
	 * @param scopes 添加应用签约的所有权限
	 * @return 对象自身
	 */
	public PkuseClientModel addContractScopes(String... scopes) {
		if(this.contractScopes == null) {
			this.contractScopes = new ArrayList<>();
		}
		this.contractScopes.addAll(Arrays.asList(scopes));
		return this;
	}

	/**
	 * @param redirectUris 添加应用允许授权的所有 redirect_uri
	 * @return 对象自身
	 */
	public PkuseClientModel addAllowRedirectUris(String... redirectUris) {
		if(this.allowRedirectUris == null) {
			this.allowRedirectUris = new ArrayList<>();
		}
		this.allowRedirectUris.addAll(Arrays.asList(redirectUris));
		return this;
	}

	/**
	 * @param grantTypes 应用允许的所有 grant_type
	 * @return 对象自身
	 */
	public PkuseClientModel addAllowGrantTypes(String... grantTypes) {
		if(this.allowGrantTypes == null) {
			this.allowGrantTypes = new ArrayList<>();
		}
		this.allowGrantTypes.addAll(Arrays.asList(grantTypes));
		return this;
	}


	// get set

	/**
	 * @return 应用id
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * @param clientId 应用id 
	 * @return 对象自身 
	 */
	public PkuseClientModel setClientId(String clientId) {
		this.clientId = clientId;
		return this;
	}

	/**
	 * @return 应用秘钥 
	 */
	public String getClientSecret() {
		return clientSecret;
	}

	/**
	 * @param clientSecret 应用秘钥 
	 * @return 对象自身 
	 */
	public PkuseClientModel setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
		return this;
	}

	/**
	 * @return 应用签约的所有权限
	 */
	public List<String> getContractScopes() {
		return contractScopes;
	}

	/**
	 * @param contractScopes 应用签约的所有权限
	 * @return 对象自身 
	 */
	public PkuseClientModel setContractScopes(List<String> contractScopes) {
		this.contractScopes = contractScopes;
		return this;
	}

	/**
	 * @return 应用允许授权的所有 redirect_uri
	 */
	public List<String> getAllowRedirectUris() {
		return allowRedirectUris;
	}

	/**
	 * @param allowRedirectUris 应用允许授权的所有 redirect_uri
	 * @return 对象自身 
	 */
	public PkuseClientModel setAllowRedirectUris(List<String> allowRedirectUris) {
		this.allowRedirectUris = allowRedirectUris;
		return this;
	}

	/**
	 * @return 应用允许的所有 grant_type
	 */
	public List<String> getAllowGrantTypes() {
		return allowGrantTypes;
	}

	/**
	 * 应用允许的所有 grant_type
	 * @param allowGrantTypes /
	 * @return /
	 */
	public PkuseClientModel setAllowGrantTypes(List<String> allowGrantTypes) {
		this.allowGrantTypes = allowGrantTypes;
		return this;
	}

	/**
	 * 获取 主体id
	 *
	 * @return subjectId 主体id
	 */
	public String getSubjectId() {
		return this.subjectId;
	}

	/**
	 * 设置 主体id
	 *
	 * @param subjectId 主体id
	 */
	public PkuseClientModel setSubjectId(String subjectId) {
		this.subjectId = subjectId;
		return this;
	}

	/**
	 * @return 此应用 是否在每次 Refresh-Token 刷新 Access-Token 时，产生一个新的 Refresh-Token [默认取全局配置]
	 */
	public Boolean getIsNewRefresh() {
		return isNewRefresh;
	}
	
	/**
	 * @param isNewRefresh 此应用 是否在每次 Refresh-Token 刷新 Access-Token 时，产生一个新的 Refresh-Token [默认取全局配置]
	 * @return 对象自身 
	 */
	public PkuseClientModel setIsNewRefresh(Boolean isNewRefresh) {
		this.isNewRefresh = isNewRefresh;
		return this;
	}
	
	/**
	 * @return 此应用 Access-Token 保存的时间(单位秒)  [默认取全局配置]
	 */
	public long getAccessTokenTimeout() {
		return accessTokenTimeout;
	}
	
	/**
	 * @param accessTokenTimeout 此应用 Access-Token 保存的时间(单位秒)  [默认取全局配置]
	 * @return 对象自身 
	 */
	public PkuseClientModel setAccessTokenTimeout(long accessTokenTimeout) {
		this.accessTokenTimeout = accessTokenTimeout;
		return this;
	}
	
	/**
	 * @return 此应用 Refresh-Token 保存的时间(单位秒) [默认取全局配置]
	 */
	public long getRefreshTokenTimeout() {
		return refreshTokenTimeout;
	}
	
	/**
	 * @param refreshTokenTimeout 此应用 Refresh-Token 保存的时间(单位秒) [默认取全局配置]
	 * @return 对象自身 
	 */
	public PkuseClientModel setRefreshTokenTimeout(long refreshTokenTimeout) {
		this.refreshTokenTimeout = refreshTokenTimeout;
		return this;
	}
	
	/**
	 * @return 此应用 Client-Token 保存的时间(单位秒) [默认取全局配置]
	 */
	public long getClientTokenTimeout() {
		return clientTokenTimeout;
	}
	
	/**
	 * @param clientTokenTimeout 此应用 Client-Token 保存的时间(单位秒) [默认取全局配置]
	 * @return 对象自身 
	 */
	public PkuseClientModel setClientTokenTimeout(long clientTokenTimeout) {
		this.clientTokenTimeout = clientTokenTimeout;
		return this;
	}

	/**
	 * 获取 是否允许此应用自动确认授权（高危配置，禁止向不被信任的第三方开启此选项）
	 *
	 * @return /
	 */
	public Boolean getIsAutoConfirm() {
		return this.isAutoConfirm;
	}

	/**
	 * 设置 是否允许此应用自动确认授权（高危配置，禁止向不被信任的第三方开启此选项）
	 *
	 * @param isAutoConfirm /
	 * @return 对象自身
	 */
	public PkuseClientModel setIsAutoConfirm(Boolean isAutoConfirm) {
		this.isAutoConfirm = isAutoConfirm;
		return this;
	}

	/**
	 *  此应用单个用户最多同时存在的 Access-Token 数量
	 * @return /
	 */
	public int getMaxAccessTokenCount() {
		return maxAccessTokenCount;
	}

	/**
	 * 设置  此应用单个用户最多同时存在的 Access-Token 数量
	 * @param maxAccessTokenCount /
	 * @return 对象自身
	 */
	public PkuseClientModel setMaxAccessTokenCount(int maxAccessTokenCount) {
		this.maxAccessTokenCount = maxAccessTokenCount;
		return this;
	}

	/**
	 * 此应用单个用户最多同时存在的 Refresh-Token 数量
	 * @return /
	 */
	public int getMaxRefreshTokenCount() {
		return maxRefreshTokenCount;
	}

	/**
	 * 此应用单个用户最多同时存在的 Refresh-Token 数量
	 * @param maxRefreshTokenCount /
	 * @return 对象自身
	 */
	public PkuseClientModel setMaxRefreshTokenCount(int maxRefreshTokenCount) {
		this.maxRefreshTokenCount = maxRefreshTokenCount;
		return this;
	}

	/**
	 * 此应用单个用户最多同时存在的 Client-Token 数量
	 * @return /
	 */
	public int getMaxClientTokenCount() {
		return maxClientTokenCount;
	}

	/**
	 * 此应用单个用户最多同时存在的 Client-Token 数量
	 * @param maxClientTokenCount /
	 * @return 对象自身
	 */
	public PkuseClientModel setMaxClientTokenCount(int maxClientTokenCount) {
		this.maxClientTokenCount = maxClientTokenCount;
		return this;
	}

	@Override
	public String toString() {
		return "PkuseClientModel{" +
				"clientId='" + clientId + '\'' +
				", clientSecret='" + clientSecret + '\'' +
				", contractScopes=" + contractScopes +
				", allowRedirectUris=" + allowRedirectUris +
				", allowGrantTypes=" + allowGrantTypes +
				", subjectId=" + subjectId +
				", isNewRefresh=" + isNewRefresh +
				", accessTokenTimeout=" + accessTokenTimeout +
				", refreshTokenTimeout=" + refreshTokenTimeout +
				", clientTokenTimeout=" + clientTokenTimeout +
				", isAutoConfirm=" + isAutoConfirm +
				", maxAccessTokenCount=" + maxAccessTokenCount +
				", refreshTokenTimeout=" + refreshTokenTimeout +
				", maxClientTokenCount=" + maxClientTokenCount +
				'}';
	}

}
