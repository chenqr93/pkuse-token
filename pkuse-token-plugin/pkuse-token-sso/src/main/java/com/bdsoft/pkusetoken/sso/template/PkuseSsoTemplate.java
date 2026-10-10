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
package com.bdsoft.pkusetoken.sso.template;

import com.bdsoft.pkusetoken.sso.message.PkuseSsoMessage;
import com.bdsoft.pkusetoken.sso.message.PkuseSsoMessageHolder;
import com.bdsoft.pkusetoken.sso.name.ApiName;
import com.bdsoft.pkusetoken.sso.name.ParamName;
import com.bdsoft.pkusetoken.stp.StpLogic;
import com.bdsoft.pkusetoken.stp.StpUtil;

/**
 * SSO 模板方法类 （公共端）
 *
 * @author click33
 * @since 1.30.0
 */
public class PkuseSsoTemplate {

	// ---------------------- 全局配置 ---------------------- 

	/**
	 * 所有 API 名称 
	 */
	public ApiName apiName = new ApiName();
	
	/**
	 * 所有参数名称 
	 */
	public ParamName paramName = new ParamName();

	/**
	 * @param paramName 替换 paramName 对象 
	 * @return 对象自身
	 */
	public PkuseSsoTemplate setParamName(ParamName paramName) {
		this.paramName = paramName;
		return this;
	}
	
	/**
	 * @param apiName 替换 apiName 对象 
	 * @return 对象自身
	 */
	public PkuseSsoTemplate setApiName(ApiName apiName) {
		this.apiName = apiName;
		return this;
	}

	/**
	 * 底层使用的 StpLogic 对象
	 */
	StpLogic stpLogic;

	/**
	 * 写入底层使用的会话对象
	 *
	 * @param stpLogic /
	 * @return /
	 */
	public PkuseSsoTemplate setStpLogic(StpLogic stpLogic) {
		this.stpLogic = stpLogic;
		return this;
	}

	/**
	 * 获取底层使用的会话对象
	 * @return /
	 */
	public StpLogic getStpLogic() {
		return this.stpLogic;
	}

	/**
	 * 获取底层使用的会话对象，如果没有配置则返回全局默认 StpLogic
	 * @return /
	 */
	public StpLogic getStpLogicOrGlobal() {
		StpLogic stpLogic = getStpLogic();
		if (stpLogic == null) {
			return StpUtil.stpLogic;
		}
		return stpLogic;
	}

	// ----------- 消息处理

	/**
	 * SSO 消息处理器 - 持有器
	 */
	public PkuseSsoMessageHolder messageHolder = new PkuseSsoMessageHolder();

	/**
	 * 处理指定消息
	 *
	 * @param message /
	 */
	public Object handleMessage(PkuseSsoMessage message) {
		return messageHolder.handleMessage(this, message);
	}

}
