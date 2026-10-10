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
import com.bdsoft.pkusetoken.jwt.error.PkuseJwtErrorCode;
import com.bdsoft.pkusetoken.jwt.exception.PkuseJwtException;
import com.bdsoft.pkusetoken.listener.PkuseTokenEventCenter;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLoginParameter;
import com.bdsoft.pkusetoken.stp.PkuseTokenInfo;
import com.bdsoft.pkusetoken.stp.StpLogic;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;

import java.util.Map;

/**
 * Sa-Token 整合 jwt -- Stateless 无状态模式
 * 
 * @author click33
 * @since 1.30.0
 */
public class StpLogicJwtForStateless extends StpLogic {

	/**
	 * Sa-Token 整合 jwt -- Stateless 无状态 
	 */
	public StpLogicJwtForStateless() {
		super(StpUtil.TYPE);
	}

	/**
	 * Sa-Token 整合 jwt -- Stateless 无状态 
	 * @param loginType 账号体系标识 
	 */
	public StpLogicJwtForStateless(String loginType) {
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
	 * 创建指定账号id的登录会话
	 * @param id 登录id，建议的类型：（long | int | String）
	 * @param loginParameter 此次登录的参数Model 
	 * @return 返回会话令牌 
	 */
	@Override
	public String createLoginSession(Object id, PkuseLoginParameter loginParameter) {

		// 1、先检查一下，传入的参数是否有效
		checkLoginArgs(id, loginParameter);

		// 3、生成一个token
		String tokenValue = createTokenValue(id, loginParameter.getDeviceType(), loginParameter.getTimeout(), loginParameter.getExtraData());
		
		// 4、$$ 发布事件：账号xxx 登录成功
		PkuseTokenEventCenter.doLogin(loginType, id, tokenValue, loginParameter);

		// 5、返回
		return tokenValue;
	}

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
		// 如果连token都没有，那么无需执行任何操作 
		String tokenValue = getTokenValue();
 		if(PkuseFoxUtil.isEmpty(tokenValue)) {
 			return;
 		}

 		// 从当前 [storage存储器] 里删除 
 		PkuseHolder.getStorage().delete(splicingKeyJustCreatedSave());
 		
 		// 如果打开了Cookie模式，则把cookie清除掉 
 		if(getConfigOrGlobal().getIsReadCookie()){
 			PkuseHolder.getResponse().deleteCookie(getTokenName());
		}
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

 	
 	// ------------------- 过期时间相关 -------------------  

 	/**
 	 * 获取指定 token 剩余有效时间 (单位: 秒)
 	 */
	@Override
 	public long getTokenTimeout(String tokenValue) {
 		return PkuseJwtUtil.getTimeout(getTokenValue(), loginType, jwtSecretKey());
 	}
 	
 	
 	// ------------------- id 反查 token 相关操作 -------------------  

	/**
	 * 返回当前会话的登录设备类型 
	 * @return 当前令牌的登录设备类型
	 */
	@Override
	public String getLoginDeviceType() {
		// 如果没有token，直接返回 null 
		String tokenValue = getTokenValue();
		if(tokenValue == null) {
			return null;
		}
		// 如果还未登录，直接返回 null 
		if(!isLogin()) {
			return null;
		}
		// 获取
		return PkuseJwtUtil.getPayloadsNotCheck(tokenValue, loginType, jwtSecretKey()).getStr(PkuseJwtUtil.DEVICE_TYPE);
	}

	
	// ------------------- Bean对象代理 -------------------  
	
	/**
	 * [禁用] 返回持久化对象 
	 */
	@Override
	public PkuseTokenDao getSaTokenDao() {
		throw new ApiDisabledException();
	}

	/**
	 * 重写返回：支持 extra 扩展参数
	 */
	@Override
	public boolean isSupportExtra() {
		return true;
	}

}
