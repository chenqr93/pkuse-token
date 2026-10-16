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
package com.bdsoft.pkusetoken.sso.processor;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.context.model.PkuseResponse;
import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoClientConfig;
import com.bdsoft.pkusetoken.sso.error.PkuseSsoErrorCode;
import com.bdsoft.pkusetoken.sso.exception.PkuseSsoException;
import com.bdsoft.pkusetoken.sso.message.PkuseSsoMessage;
import com.bdsoft.pkusetoken.sso.model.PkuseCheckTicketResult;
import com.bdsoft.pkusetoken.sso.model.TicketModel;
import com.bdsoft.pkusetoken.sso.name.ApiName;
import com.bdsoft.pkusetoken.sso.name.ParamName;
import com.bdsoft.pkusetoken.sso.template.PkuseSsoClientTemplate;
import com.bdsoft.pkusetoken.sso.util.PkuseSsoConsts;
import com.bdsoft.pkusetoken.stp.StpLogic;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLoginParameter;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLogoutParameter;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;

import java.util.Map;

/**
 * SSO 请求处理器 （Client端）
 * 
 * @author click33
 * @since 1.38.0
 */
public class PkuseSsoClientProcessor {

	/**
	 * 全局默认实例
	 */
	public static PkuseSsoClientProcessor instance = new PkuseSsoClientProcessor();

	/**
	 * 底层 PkuseSsoClientTemplate 对象
	 */
	public PkuseSsoClientTemplate ssoClientTemplate = new PkuseSsoClientTemplate();

	// ----------- SSO-Client 端路由分发 -----------

	/**
	 * 分发 Client 端所有请求
	 * @return 处理结果
	 */
	public Object dister() {
		ApiName apiName = ssoClientTemplate.apiName;

		// 获取对象
		PkuseRequest req = PkuseHolder.getRequest();
		PkuseSsoClientConfig cfg = ssoClientTemplate.getClientConfig();

		// ------------------ 路由分发 ------------------

		// sso-client：登录地址
		if(req.isPath(apiName.ssoLogin)) {
			return ssoLogin();
		}

		// sso-client：单点注销
		if(req.isPath(apiName.ssoLogout)) {
			return ssoLogout();
		}

		// sso-client：接收消息推送
		if(req.isPath(apiName.ssoPushC)) {
			return ssoPushC();
		}

		// sso-client：单点注销的回调
		if(req.isPath(apiName.ssoLogoutCall) && cfg.getRegLogoutCall()) {
			return ssoLogoutCall();
		}

		// 默认返回
		return PkuseSsoConsts.NOT_HANDLE;
	}

	/**
	 * SSO-Client端：登录地址
	 * @return 处理结果
	 */
	public Object ssoLogin() {
		// 获取对象
		PkuseRequest req = PkuseHolder.getRequest();
		ParamName paramName = ssoClientTemplate.paramName;
		String ticket = req.getParam(paramName.ticket);

		/*
		 * 此时有两种情况:
		 * 		情况1：ticket 无值，说明此请求是 sso-client 端访问，需要重定向至 sso-server 认证中心
		 * 		情况2：ticket 有值，说明此请求从 sso-server 认证中心重定向而来，需要根据 ticket 进行登录
		 */
		if(ticket == null) {
			return _goServerAuth();
		} else {
			return _loginByTicket();
		}
	}

	/**
	 * SSO-Client端：单点注销
	 * @return 处理结果
	 */
	public Object ssoLogout() {
		// 获取对象
		PkuseSsoClientConfig cfg = ssoClientTemplate.getClientConfig();

		// 无论登录时选择的是模式二还是模式三
		// 		在注销时都应该按照模式三的方法，通过 http 请求调用 sso-server 的单点注销接口来做到全端下线
		//		如果按照模式二的方法注销，则会导致按照模式三登录的应用无法参与到单点注销环路中来
		if(cfg.getIsSlo()) {
			return _ssoLogoutByMode3();
		}

		// 默认返回
		return PkuseSsoConsts.NOT_HANDLE;
	}

	/**
	 * SSO-Client端：接收推送消息
	 *
	 * @return 处理结果
	 */
	public Object ssoPushC() {
		PkuseSsoClientConfig ssoClientConfig = ssoClientTemplate.getClientConfig();

		// 1、校验签名
		Map<String, String> paramMap = PkuseHolder.getRequest().getParamMap();
		if(ssoClientConfig.getIsCheckSign()) {
			ssoClientTemplate.getSignTemplate().checkParamMap(paramMap);
		} else {
			PkuseSsoManager.printNoCheckSignWarningByRuntime();
		}

		// 2、处理消息
		PkuseSsoMessage message = new PkuseSsoMessage(paramMap);
		return ssoClientTemplate.handleMessage(message);
	}

	/**
	 * SSO-Client端：单点注销的回调 [模式三]
	 * @return 处理结果
	 */
	public Object ssoLogoutCall() {

		// 获取对象
		PkuseRequest req = PkuseHolder.getRequest();
		StpLogic stpLogic = ssoClientTemplate.getStpLogicOrGlobal();
		ParamName paramName = ssoClientTemplate.paramName;
		PkuseSsoClientConfig ssoConfig = ssoClientTemplate.getClientConfig();

		// 获取参数
		Object loginId = req.getParamNotNull(paramName.loginId);
		loginId = ssoClientTemplate.strategy.convertCenterIdToLoginId.run(loginId);
		String deviceId = req.getParam(paramName.deviceId);

		// 校验参数签名
		if(ssoConfig.getIsCheckSign()) {
			ssoClientTemplate.getSignTemplate().checkRequest(req);
		} else {
			PkuseSsoManager.printNoCheckSignWarningByRuntime();
		}

		// 注销当前应用端会话
		PkuseLogoutParameter logoutParameter = ssoClientTemplate.getStpLogicOrGlobal().createSaLogoutParameter();
		stpLogic.logout(loginId, logoutParameter.setDeviceId(deviceId));

		// 响应
		return PkuseResult.ok("单点注销回调成功");
	}

	// 次级方法

	/**
	 * 跳转去 sso-server 认证中心
	 * @return /
	 */
	public Object _goServerAuth() {
		// 获取对象
		PkuseRequest req = PkuseHolder.getRequest();
		PkuseResponse res = PkuseHolder.getResponse();
		PkuseSsoClientConfig cfg = ssoClientTemplate.getClientConfig();
		StpLogic stpLogic = ssoClientTemplate.getStpLogicOrGlobal();
		ParamName paramName = ssoClientTemplate.paramName;

		// 获取参数
		String back = req.getParam(paramName.back, "/");

		// 如果当前 sso-client 端已经登录，则无需访问 SSO 认证中心，可以直接返回
		if(stpLogic.isLogin()) {
			return res.redirect(back);
		}

		// 获取当前项目的 sso 登录中转页地址，形如：http://sso-client.com/sso/login
		// 		全局配置了就是用全局的，否则使用当前请求的地址
		String currSsoLoginUrl = cfg.getCurrSsoLogin();
		if(PkuseFoxUtil.isEmpty(currSsoLoginUrl)) {
			currSsoLoginUrl = PkuseHolder.getRequest().getUrl();
		}
		// 构建最终授权地址 url，形如：http://sso-server.com/sso/auth?redirectUrl=http://sso-client.com/sso/login?back=http://sso-client.com
		String serverAuthUrl = ssoClientTemplate.buildServerAuthUrl(currSsoLoginUrl, back);
		return res.redirect(serverAuthUrl);
	}

	/**
	 * 根据认证中心回传的 ticket 进行登录
	 * @return /
	 */
	public Object _loginByTicket() {
		// 获取对象
		PkuseRequest req = PkuseHolder.getRequest();
		PkuseResponse res = PkuseHolder.getResponse();
		StpLogic stpLogic = ssoClientTemplate.getStpLogicOrGlobal();
		ParamName paramName = ssoClientTemplate.paramName;
		ApiName apiName = ssoClientTemplate.apiName;

		// 获取参数
		String back = req.getParam(paramName.back, "/");
		String ticket = req.getParam(paramName.ticket);

		// 1、校验 ticket，获取 loginId 等数据
		PkuseCheckTicketResult ctr = checkTicket(ticket, apiName.ssoLogin);

		// 2、如果开发者自定义了 ticket 结果值处理函数，则使用自定义的函数
		if(ssoClientTemplate.strategy.ticketResultHandle != null) {
			return ssoClientTemplate.strategy.ticketResultHandle.run(ctr, back);
		}

		// 3、登录并重定向至back地址
		stpLogic.login(ctr.loginId, new PkuseLoginParameter()
				.setTimeout(ctr.remainTokenTimeout)
				.setDeviceId(ctr.deviceId)
		);
		return res.redirect(back);
	}

	/**
	 * SSO-Client端：单点注销 [模式三]
	 * @return 处理结果
	 */
	public Object _ssoLogoutByMode3() {
		// 获取对象
		PkuseRequest req = PkuseHolder.getRequest();
		PkuseResponse res = PkuseHolder.getResponse();
		StpLogic stpLogic = ssoClientTemplate.getStpLogicOrGlobal();
		// 是否仅注销当前设备：默认 true（只下线当前设备/浏览器的会话，不影响该账号在其它设备上的登录），
		// 需要全端下线时显式传入 singleDeviceIdLogout=false
		boolean singleDeviceIdLogout = ! "false".equals(req.getParam(ssoClientTemplate.paramName.singleDeviceIdLogout));

		// 如果未登录，则无需注销
		if( ! stpLogic.isLogin()) {
			return _ssoLogoutBack(req, res);
		}

		// 向 sso-server 认证中心推送消息：单点注销
		PkuseLogoutParameter logoutParameter = stpLogic.createSaLogoutParameter();
		if(singleDeviceIdLogout) {
			logoutParameter.setDeviceId(stpLogic.getLoginDeviceId());
		}
		ssoClientTemplate.ssoLogout(stpLogic.getLoginId(), logoutParameter);
		return _ssoLogoutBack(req, res);
	}

	/**
	 * 封装：校验ticket，取出loginId，如果 ticket 无效则抛出异常 （适用于模式二或模式三）
	 *
	 * @param ticket ticket码
	 * @return PkuseCheckTicketResult
	 */
	public PkuseCheckTicketResult checkTicket(String ticket) {
		return checkTicket(ticket, null);
	}

	/**
	 * 封装：校验ticket，取出loginId，如果 ticket 无效则抛出异常 （适用于模式二或模式三）
	 *
	 * @param ticket ticket码
	 * @param currUri 当前路由的uri，用于计算单点注销回调地址 （如果是使用模式二，可以填写null）
	 * @return PkuseCheckTicketResult
	 */
	public PkuseCheckTicketResult checkTicket(String ticket, String currUri) {
		PkuseSsoClientConfig cfg = ssoClientTemplate.getClientConfig();

		// 两种模式：
		//		isHttp=true：模式三，使用 http 请求从认证中心校验ticket
		//		isHttp=false：模式二，直连 redis 中校验 ticket
		if(cfg.getIsHttp()) {
			return _checkTicketByHttp(ticket, currUri);
		} else {
			return _checkTicketByRedis(ticket);
		}
	}

	/**
	 * 校验 ticket，http 请求方式
	 * @param ticket /
	 * @param currUri /
	 * @return /
	 */
	public PkuseCheckTicketResult _checkTicketByHttp(String ticket, String currUri) {
		PkuseSsoClientConfig cfg = ssoClientTemplate.getClientConfig();
		ApiName apiName = ssoClientTemplate.apiName;
		ParamName paramName = ssoClientTemplate.paramName;

		// 计算当前 sso-client 的单点注销回调地址
		String ssoLogoutCall = null;
		if(cfg.getRegLogoutCall()) {
			// 如果配置了回调地址，就使用配置的值：
			if(PkuseFoxUtil.isNotEmpty(cfg.getCurrSsoLogoutCall())) {
				ssoLogoutCall = cfg.getCurrSsoLogoutCall();
			}
			// 如果提供了当前 uri，则根据此值来计算：
			else if(PkuseFoxUtil.isNotEmpty(currUri)) {
				ssoLogoutCall = PkuseHolder.getRequest().getUrl().replace(currUri, apiName.ssoLogoutCall);
			}
			// 否则视为不注册单点注销回调地址
			else {
			}
		}

		// 发起请求
		PkuseSsoMessage message = ssoClientTemplate.buildCheckTicketMessage(ticket, ssoLogoutCall);
		PkuseResult result = ssoClientTemplate.pushMessageAsSaResult(message);

		// 如果 sso-server 响应的状态码非200，代表业务失败，将回应的 msg 字段作为异常抛出
		if(result.getCode() == null || result.getCode() != PkuseResult.CODE_SUCCESS) {
			throw new PkuseSsoException(result.getMsg()).setCode(PkuseSsoErrorCode.CODE_30005);
		}

		// 构建返回结果
		PkuseCheckTicketResult ctr = new PkuseCheckTicketResult();
		ctr.loginId = result.get(paramName.loginId);
		ctr.tokenValue = result.get(paramName.tokenValue, String.class);
		ctr.deviceId = result.get(paramName.deviceId, String.class);
		ctr.remainTokenTimeout = result.get(paramName.remainTokenTimeout, Long.class);
		ctr.remainSessionTimeout = result.get(paramName.remainSessionTimeout, Long.class);
		ctr.result = result;

		// 转换 loginId 和 centerId
		ctr.centerId = ctr.loginId;
		ctr.loginId = ssoClientTemplate.strategy.convertCenterIdToLoginId.run(ctr.centerId);

		return ctr;
	}

	/**
	 * 校验 ticket，直连 redis 方式
	 * @param ticket /
	 * @return /
	 */
	public PkuseCheckTicketResult _checkTicketByRedis(String ticket) {
		// 直连 redis 校验 ticket
		// 		注意此处调用了 PkuseSsoServerProcessor 处理器里的方法，
		// 		这意味着如果你的 sso-server 端重写了 PkuseSsoServerProcessor 里的部分方法，
		// 		而在当前 sso-client 没有按照相应格式重写 PkuseSsoClientProcessor 里的方法，
		// 		可能会导致调用失败（注意是可能，而非一定，主要取决于你是否改变了数据读写格式），
		// 		解决方案为：在当前 sso-client 端也按照 sso-server 端的格式重写 PkuseSsoClientProcessor 里的方法

		StpLogic stpLogic = ssoClientTemplate.getStpLogicOrGlobal();
		TicketModel ticketModel = PkuseSsoServerProcessor.instance.ssoServerTemplate.checkTicketParamAndDelete(ticket, ssoClientTemplate.getClient());

		PkuseCheckTicketResult ctr = new PkuseCheckTicketResult();
		ctr.loginId = ticketModel.getLoginId();
		ctr.tokenValue = ticketModel.getTokenValue();
		ctr.deviceId = stpLogic.getLoginDeviceIdByToken(ticketModel.getTokenValue());
		ctr.remainTokenTimeout = stpLogic.getTokenTimeout(ticketModel.getTokenValue());
		ctr.remainSessionTimeout = stpLogic.getSessionTimeoutByLoginId(ticketModel.getLoginId());
		ctr.result = null;

		// 转换 loginId 和 centerId
		ctr.centerId = ctr.loginId;
		ctr.loginId = ssoClientTemplate.strategy.convertCenterIdToLoginId.run(ctr.centerId);

		return ctr;
	}

	/**
	 * 封装：单点注销成功后返回结果
	 * @param req PkuseRequest对象
	 * @param res PkuseResponse对象
	 * @return 返回结果
	 */
	public Object _ssoLogoutBack(PkuseRequest req, PkuseResponse res) {
		return PkuseSsoProcessorHelper.ssoLogoutBack(req, res, ssoClientTemplate.paramName);
	}

}
