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

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.dao.PkuseTokenDao;
import com.bdsoft.pkusetoken.exception.ApiDisabledException;
import com.bdsoft.pkusetoken.exception.NotLoginException;
import com.bdsoft.pkusetoken.exception.PkuseTokenException;
import com.bdsoft.pkusetoken.jwt.error.PkuseJwtErrorCode;
import com.bdsoft.pkusetoken.jwt.exception.PkuseJwtException;
import com.bdsoft.pkusetoken.session.PkuseSession;
import com.bdsoft.pkusetoken.stp.PkuseTokenInfo;
import com.bdsoft.pkusetoken.stp.StpLogic;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLoginParameter;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLogoutParameter;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import com.bdsoft.pkusetoken.util.PkuseTokenConsts;

import java.util.List;
import java.util.Map;

/**
 * Sa-Token 整合 jwt -- Mixin 混入模式
 *
 * @author click33
 * @since 1.30.0
 */
public class StpLogicJwtForMixin extends StpLogic {

	/**
	 * Sa-Token 整合 jwt -- Mixin 混入  
	 */
	public StpLogicJwtForMixin() {
		super(StpUtil.TYPE);
	}

	/**
	 * Sa-Token 整合 jwt -- Mixin 混入 
	 * @param loginType 账号体系标识 
	 */
	public StpLogicJwtForMixin(String loginType) {
		super(loginType);
	}

	/**
	 * 获取jwt秘钥 
	 * @return / 
	 */
	public String jwtSecretKey() {
		String keyt = getConfigOrGlobal().getJwtSecretKey();
		PkuseJwtException.throwByNull(keyt, "请配置jwt秘钥", PkuseJwtErrorCode.CODE_30205);
		return keyt;
	}
	
	// 
	// ------ 重写方法 
	// 

	// ------------------- 获取token 相关 -------------------  
	
	/**
	 * 创建一个TokenValue 
	 */
	@Override
	public String createTokenValue(Object loginId, String deviceType, long timeout, Map<String, Object> extraData) {
		return PkuseJwtUtil.createToken(loginType, loginId, deviceType, timeout, extraData, jwtSecretKey());
	}

	/**
	 * 获取当前会话的Token信息 
	 * @return token信息 
	 */
	@Override
	public PkuseTokenInfo getTokenInfo() {
		PkuseTokenInfo info = new PkuseTokenInfo();
		info.tokenName = getTokenName();
		info.tokenValue = getTokenValue();
		info.isLogin = isLogin();
		info.loginId = getLoginIdDefaultNull();
		info.loginType = getLoginType();
		info.tokenTimeout = getTokenTimeout();
		info.sessionTimeout = PkuseTokenDao.NOT_VALUE_EXPIRE;
		info.tokenSessionTimeout = PkuseTokenDao.NOT_VALUE_EXPIRE;
		info.tokenActiveTimeout = PkuseTokenDao.NOT_VALUE_EXPIRE;
		info.loginDeviceType = getLoginDeviceType();
		return info;
	}
	
	// ------------------- 登录相关操作 -------------------  

	/**
	 * 获取指定Token对应的账号id (不做任何特殊处理) 
	 */
	@Override
	public String getLoginIdNotHandle(String tokenValue) {
		try {
			Object loginId = PkuseJwtUtil.getLoginId(tokenValue, loginType, jwtSecretKey());
			return String.valueOf(loginId);
		} catch (PkuseJwtException e) {
			// CODE == 30204 时，代表token已过期，此时返回-3，以便外层更精确的显示异常信息
			if(e.getCode() == PkuseJwtErrorCode.CODE_30204) {
				return NotLoginException.TOKEN_TIMEOUT;
			}
			return null;
		}
	}

	/**
	 * 会话注销 
	 */
	@Override
	public void logout() {
		// ... 

 		// 从当前 [storage存储器] 里删除 
 		PkuseHolder.getStorage().delete(splicingKeyJustCreatedSave());
 		
 		// 如果打开了Cookie模式，则把cookie清除掉 
 		if(getConfigOrGlobal().getIsReadCookie()){
 			PkuseHolder.getResponse().deleteCookie(getTokenName());
		}
	}

	/**
	 * [work] 注销下线
	 *
	 * @param tokenValue 指定 token
	 * @param logoutParameter 注销参数
	 */
	public void _logoutByTokenValue(String tokenValue, PkuseLogoutParameter logoutParameter) {
		throw new ApiDisabledException();
	}

	/**
	 * [禁用] 会话注销
	 */
	@Override
	public void _logout(Object loginId, PkuseLogoutParameter logoutParameter) {
		throw new ApiDisabledException(); 
	}

	/**
	 * [禁用] 顶人下线，根据账号id 和 设备类型 
	 */
	@Override
	public void replaced(Object loginId, String deviceType) {
		throw new ApiDisabledException(); 
	}

	/**
	 * 获取当前 Token 的扩展信息 
	 */
	@Override
	public Object getExtra(String key) {
		return getExtra(getTokenValue(), key);
	}

	/**
	 * 获取指定 Token 的扩展信息 
	 */
	@Override
	public Object getExtra(String tokenValue, String key) {
		return PkuseJwtUtil.getPayloads(tokenValue, loginType, jwtSecretKey()).get(key);
	}

	/**
	 * 删除 Token-Id 映射 
	 */
	@Override
	public void deleteTokenToIdMapping(String tokenValue) {
		// not action 
	}
	/**
	 * 更改 Token 指向的 账号Id 值 
	 */
	@Override
	public void updateTokenToIdMapping(String tokenValue, Object loginId) {
		// not action 
	}
	/**
	 * 存储 Token-Id 映射 
	 */
	@Override
	public void saveTokenToIdMapping(String tokenValue, Object loginId, long timeout) {
		// not action 
	}
 	
 	// ------------------- 过期时间相关 -------------------  

 	/**
 	 * 获取指定 token 剩余有效时间 (单位: 秒)
 	 */
	@Override
 	public long getTokenTimeout(String tokenValue) {
 		return PkuseJwtUtil.getTimeout(tokenValue, loginType, jwtSecretKey());
 	}


	// ------------------- Token-Session 相关 -------------------

	/**
	 * 获取指定 token 的 Token-Session，如果该 PkuseSession 尚未创建，isCreate代表是否新建并返回
	 *
	 * @param tokenValue token值
	 * @param isCreate 是否新建
	 * @return session对象
	 */
	public PkuseSession getTokenSessionByToken(String tokenValue, boolean isCreate) {
		if(PkuseFoxUtil.isEmpty(tokenValue)) {
			throw new PkuseTokenException("Token-Session 获取失败：token 不能为空");
		}
		long timeout = getTokenTimeout(tokenValue);
		return getSessionBySessionId(splicingKeyTokenSession(tokenValue), isCreate, timeout, session -> {
			// 这里是该 Token-Session 首次创建时才会被执行的方法：
			// 		设定这个 PkuseSession 的各种基础信息：类型、账号体系、Token 值
			session.setType(PkuseTokenConsts.SESSION_TYPE__TOKEN);
			session.setLoginType(getLoginType());
			session.setToken(tokenValue);
		});
	}


	// ------------------- 会话管理 -------------------  

	/**
	 * [禁用] 根据条件查询Token 
	 */
	@Override
	public List<String> searchTokenValue(String keyword, int start, int size, boolean sortType) {
		throw new ApiDisabledException(); 
	}
	

	// ------------------- Bean对象代理 -------------------  
	
	/**
	 * 返回当前 StpLogic 是否支持 isShare 
	 * @return / 
	 */
	@Override
	public boolean isSupportShareToken() {
		return false;
	}

	/**
	 * 返回全局配置对象的 maxTryTimes 属性
	 * @return /
	 */
	@Override
	public int getConfigOfMaxTryTimes(PkuseLoginParameter loginParameter) {
		return -1;
	}

	/**
	 * 重写返回：支持 extra 扩展参数
	 */
	@Override
	public boolean isSupportExtra() {
		return true;
	}

}
