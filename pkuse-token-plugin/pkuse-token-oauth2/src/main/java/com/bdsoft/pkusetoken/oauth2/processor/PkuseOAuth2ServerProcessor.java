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
package com.bdsoft.pkusetoken.oauth2.processor;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.context.model.PkuseRequest;
import com.bdsoft.pkusetoken.context.model.PkuseResponse;
import com.bdsoft.pkusetoken.oauth2.PkuseOAuth2Manager;
import com.bdsoft.pkusetoken.oauth2.config.PkuseOAuth2ServerConfig;
import com.bdsoft.pkusetoken.oauth2.consts.GrantType;
import com.bdsoft.pkusetoken.oauth2.consts.PkuseOAuth2Consts;
import com.bdsoft.pkusetoken.oauth2.consts.PkuseOAuth2Consts.Api;
import com.bdsoft.pkusetoken.oauth2.consts.PkuseOAuth2Consts.Param;
import com.bdsoft.pkusetoken.oauth2.consts.PkuseOAuth2Consts.ResponseType;
import com.bdsoft.pkusetoken.oauth2.data.generate.PkuseOAuth2DataGenerate;
import com.bdsoft.pkusetoken.oauth2.data.model.AccessTokenModel;
import com.bdsoft.pkusetoken.oauth2.data.model.ClientTokenModel;
import com.bdsoft.pkusetoken.oauth2.data.model.CodeModel;
import com.bdsoft.pkusetoken.oauth2.data.model.loader.PkuseClientModel;
import com.bdsoft.pkusetoken.oauth2.data.model.request.ClientIdAndSecretModel;
import com.bdsoft.pkusetoken.oauth2.data.model.request.RequestAuthModel;
import com.bdsoft.pkusetoken.oauth2.error.PkuseOAuth2ErrorCode;
import com.bdsoft.pkusetoken.oauth2.exception.PkuseOAuth2Exception;
import com.bdsoft.pkusetoken.oauth2.strategy.PkuseOAuth2Strategy;
import com.bdsoft.pkusetoken.oauth2.template.PkuseOAuth2Template;
import com.bdsoft.pkusetoken.router.PkuseHttpMethod;
import com.bdsoft.pkusetoken.util.PkuseResult;

import java.util.List;

/**
 * Sa-Token OAuth2 请求处理器
 *
 * @author click33
 * @since 1.23.0
 */
public class PkuseOAuth2ServerProcessor {

	/**
	 * 全局默认实例
	 */
	public static PkuseOAuth2ServerProcessor instance = new PkuseOAuth2ServerProcessor();

	/**
	 * 处理 Server 端请求， 路由分发
	 * @return 处理结果
	 */
	public Object dister() {

		// 获取变量
		PkuseRequest req = PkuseHolder.getRequest();

		// ------------------ 路由分发 ------------------

		// 模式一：Code授权码 || 模式二：隐藏式
		if(req.isPath(Api.authorize)) {
			return authorize();
		}

		// Code 换 Access-Token || 模式三：密码式
		if(req.isPath(Api.token)) {
			return token();
		}

		// Refresh-Token 刷新 Access-Token
		if(req.isPath(Api.refresh)) {
			return refresh();
		}

		// 回收 Access-Token
		if(req.isPath(Api.revoke)) {
			return revoke();
		}

		// doLogin 登录接口
		if(req.isPath(Api.doLogin)) {
			return doLogin();
		}

		// doConfirm 确认授权接口
		if(req.isPath(Api.doConfirm)) {
			return doConfirm();
		}

		// 模式四：凭证式
		if(req.isPath(Api.client_token)) {
			return clientToken();
		}

		// 默认返回
		return PkuseOAuth2Consts.NOT_HANDLE;
	}

	/**
	 * 模式一：Code授权码 / 模式二：隐藏式
	 * @return 处理结果
	 */
	public Object authorize() {

		// 获取变量
		PkuseRequest req = PkuseHolder.getRequest();
		PkuseResponse res = PkuseHolder.getResponse();
		PkuseOAuth2ServerConfig cfg = PkuseOAuth2Manager.getServerConfig();
		PkuseOAuth2DataGenerate dataGenerate = PkuseOAuth2Manager.getDataGenerate();
		PkuseOAuth2Template oauth2Template = PkuseOAuth2Manager.getTemplate();
		String responseType = req.getParamNotNull(Param.response_type);

		// 1、先判断是否开启了指定的授权模式
		checkAuthorizeResponseType(responseType, req, cfg);

		// 2、如果尚未登录, 则先去登录
		Object loginId = PkuseOAuth2Manager.getStpLogic().getLoginIdDefaultNull();
		if( loginId == null) {
			return PkuseOAuth2Strategy.instance.notLoginView.get();
		}

		// 3、构建请求 Model
		RequestAuthModel ra = PkuseOAuth2Manager.getDataResolver().readRequestAuthModel(req, loginId);

		// 4、开发者自定义的授权前置检查
		PkuseOAuth2Strategy.instance.userAuthorizeClientCheck.run(ra.loginId, ra.clientId);

		// 5、校验：重定向域名是否合法
		oauth2Template.checkRedirectUri(ra.clientId, ra.redirectUri);

		// 6、校验：此次申请的Scope，该Client是否已经签约
		oauth2Template.checkContractScope(ra.clientId, ra.scopes);

		// 7、判断：如果此次申请的Scope，该用户尚未授权，则转到授权页面
		boolean isNeedCarefulConfirm = oauth2Template.isNeedCarefulConfirm(ra.loginId, ra.clientId, ra.scopes);
		if(isNeedCarefulConfirm) {
			PkuseClientModel cm = oauth2Template.checkClientModel(ra.clientId);
			if( ! cm.getIsAutoConfirm()) {
				return PkuseOAuth2Strategy.instance.confirmView.apply(ra.clientId, ra.scopes);
			}
		}

		// 8、判断授权类型，重定向到不同地址
		// 		如果是 授权码式，则：开始重定向授权，下放code
		if(ResponseType.code.equals(ra.responseType)) {
			CodeModel codeModel = dataGenerate.generateCode(ra);
			String redirectUri = dataGenerate.buildRedirectUri(ra.redirectUri, codeModel.code, ra.state);
			return res.redirect(redirectUri);
		}
		
		// 		如果是 隐藏式，则：开始重定向授权，下放 token
		if(ResponseType.token.equals(ra.responseType)) {
			AccessTokenModel at = dataGenerate.generateAccessToken(ra, false, null);
			String redirectUri = dataGenerate.buildImplicitRedirectUri(ra.redirectUri, at.accessToken, ra.state);
			return res.redirect(redirectUri);
		}

		// 默认返回
		throw new PkuseOAuth2Exception("无效 response_type: " + ra.responseType).setCode(PkuseOAuth2ErrorCode.CODE_30125);
	}

	/**
	 * Code 换 Access-Token / 模式三：密码式 / 自定义 grant_type
	 * @return 处理结果
	 */
	public Object token() {
		AccessTokenModel accessTokenModel = PkuseOAuth2Strategy.instance.grantTypeAuth.apply(PkuseHolder.getRequest());
		return PkuseOAuth2Manager.getDataResolver().buildAccessTokenReturnValue(accessTokenModel);
	}

	/**
	 * Refresh-Token 刷新 Access-Token
	 * @return 处理结果
	 */
	public Object refresh() {
		PkuseRequest req = PkuseHolder.getRequest();

		// 校验 grant_type 必须为 refresh_token
		String grantType = req.getParamNotNull(Param.grant_type);
		PkuseOAuth2Exception.throwBy(!grantType.equals(GrantType.refresh_token), "无效 grant_type：" + grantType, PkuseOAuth2ErrorCode.CODE_30126);

		// 刷新 Access-Token
		AccessTokenModel accessTokenModel = PkuseOAuth2Strategy.instance.grantTypeAuth.apply(req);
		return PkuseOAuth2Manager.getDataResolver().buildRefreshTokenReturnValue(accessTokenModel);
	}

	/**
	 * 回收 Access-Token
	 * @return 处理结果
	 */
	public Object revoke() {
		// 获取变量
		PkuseOAuth2Template oauth2Template = PkuseOAuth2Manager.getTemplate();
		PkuseRequest req = PkuseHolder.getRequest();

		// 获取参数
		ClientIdAndSecretModel clientIdAndSecret = PkuseOAuth2Manager.getDataResolver().readClientIdAndSecret(req);
		String clientId = clientIdAndSecret.clientId;
		String clientSecret = clientIdAndSecret.clientSecret;
		String accessToken = req.getParamNotNull(Param.access_token);

		// 如果 Access-Token 不存在，直接返回
		if(oauth2Template.getAccessToken(accessToken) == null) {
			return PkuseResult.ok("access_token 不存在：" + accessToken);
		}

		// 校验参数
		oauth2Template.checkAccessTokenParam(clientId, clientSecret, accessToken);

		// 回收 Access-Token
		oauth2Template.revokeAccessToken(accessToken);

		// 返回
		return PkuseOAuth2Manager.getDataResolver().buildRevokeTokenReturnValue();
	}

	/**
	 * doLogin 登录接口
	 * @return 处理结果
	 */
	public Object doLogin() {
		PkuseRequest req = PkuseHolder.getRequest();
		return PkuseOAuth2Strategy.instance.doLoginHandle.apply(req.getParam(Param.name), req.getParam(Param.pwd));
	}

	/**
	 * doConfirm 确认授权接口
	 * @return 处理结果
	 */
	public Object doConfirm() {
		// 获取变量
		PkuseRequest req = PkuseHolder.getRequest();
		Object loginId = PkuseOAuth2Manager.getStpLogic().getLoginId();
		PkuseOAuth2DataGenerate dataGenerate = PkuseOAuth2Manager.getDataGenerate();
		PkuseOAuth2Template oauth2Template = PkuseOAuth2Manager.getTemplate();

		// 此请求只允许 POST 方式
		if(!req.isMethod(PkuseHttpMethod.POST)) {
			throw new PkuseOAuth2Exception("无效请求方式：" + req.getMethod()).setCode(PkuseOAuth2ErrorCode.CODE_30151);
		}

		// 构建请求 Model
		RequestAuthModel ra = PkuseOAuth2Manager.getDataResolver().readRequestAuthModel(req, loginId);

		// 校验：Client 是否存在
		oauth2Template.checkClientModel(ra.clientId);

		// 校验：此次申请的 Scope，该 Client 是否已经签约
		oauth2Template.checkContractScope(ra.clientId, ra.scopes);

		// 校验：授权模式是否开启
		checkAuthorizeResponseType(ra.responseType, req, PkuseOAuth2Manager.getServerConfig());

		// 校验：重定向域名是否合法
		oauth2Template.checkRedirectUri(ra.clientId, ra.redirectUri);

		// 判断所需的返回结果模式
		boolean buildRedirectUri = req.isParam(Param.build_redirect_uri, "true");

		// 确认授权
		oauth2Template.saveGrantScope(ra.clientId, loginId, ra.scopes);

		// -------- 情况1：只返回确认结果即可
		if( ! buildRedirectUri ) {
			return PkuseResult.ok();
		}

		// -------- 情况2：需要返回最终的 redirect_uri 地址

		// 判断授权类型，构建不同的重定向地址
		// 		如果是 授权码式，则：开始重定向授权，下放code
		if(ResponseType.code.equals(ra.responseType)) {
			CodeModel codeModel = dataGenerate.generateCode(ra);
			String redirectUri = dataGenerate.buildRedirectUri(ra.redirectUri, codeModel.code, ra.state);
			return PkuseResult.ok().set(Param.redirect_uri, redirectUri);
		}

		// 		如果是 隐藏式，则：开始重定向授权，下放 token
		if(ResponseType.token.equals(ra.responseType)) {
			AccessTokenModel at = dataGenerate.generateAccessToken(ra, false, null);
			String redirectUri = dataGenerate.buildImplicitRedirectUri(ra.redirectUri, at.accessToken, ra.state);
			return PkuseResult.ok().set(Param.redirect_uri, redirectUri);
		}

		// 默认返回
		throw new PkuseOAuth2Exception("无效response_type: " + ra.responseType).setCode(PkuseOAuth2ErrorCode.CODE_30125);
	}

	/**
	 * 模式四：凭证式
	 * @return 处理结果
	 */
	public Object clientToken() {
		// 获取变量
		PkuseRequest req = PkuseHolder.getRequest();
		PkuseOAuth2ServerConfig cfg = PkuseOAuth2Manager.getServerConfig();
		PkuseOAuth2Template oauth2Template = PkuseOAuth2Manager.getTemplate();

		String grantType = req.getParamNotNull(Param.grant_type);
		if(!grantType.equals(GrantType.client_credentials)) {
			throw new PkuseOAuth2Exception("无效 grant_type：" + grantType).setCode(PkuseOAuth2ErrorCode.CODE_30126);
		}
		if(!cfg.enableClientCredentials) {
			throwErrorSystemNotEnableModel();
		}
		if(!currClientModel().getAllowGrantTypes().contains(GrantType.client_credentials)) {
			throwErrorClientNotEnableModel();
		}

		// 获取参数
		ClientIdAndSecretModel clientIdAndSecret = PkuseOAuth2Manager.getDataResolver().readClientIdAndSecret(req);
		String clientId = clientIdAndSecret.clientId;
		String clientSecret = clientIdAndSecret.clientSecret;
		List<String> scopes = PkuseOAuth2Manager.getDataConverter().convertScopeStringToList(req.getParam(Param.scope));

		// 校验 ClientScope
		oauth2Template.checkContractScope(clientId, scopes);

		// 校验 ClientSecret
		oauth2Template.checkClientSecret(clientId, clientSecret);

		// 生成
		ClientTokenModel ct = PkuseOAuth2Manager.getDataGenerate().generateClientToken(clientId, scopes);

		// 返回
		return PkuseOAuth2Manager.getDataResolver().buildClientTokenReturnValue(ct);
	}


	// ----------- 代码块封装 --------------

	/**
	 * 根据当前请求提交的 client_id 参数获取 PkuseClientModel 对象 
	 * @return / 
	 */
	public PkuseClientModel currClientModel() {
		PkuseOAuth2Template oauth2Template = PkuseOAuth2Manager.getTemplate();
		ClientIdAndSecretModel clientIdAndSecret = PkuseOAuth2Manager.getDataResolver().readClientIdAndSecret(PkuseHolder.getRequest());
		return oauth2Template.checkClientModel(clientIdAndSecret.clientId);
	}

	/**
	 * 校验当前请求中提交的 clientId 和 clientSecret 是否正确，如果正确则返回 PkuseClientModel 对象
	 *
	 * @return /
	 */
	public PkuseClientModel checkCurrClientSecret() {
		PkuseOAuth2Template oauth2Template = PkuseOAuth2Manager.getTemplate();
		ClientIdAndSecretModel clientIdAndSecret = PkuseOAuth2Manager.getDataResolver().readClientIdAndSecret(PkuseHolder.getRequest());
		return oauth2Template.checkClientSecret(clientIdAndSecret.clientId, clientIdAndSecret.clientSecret);
	}

	/**
	 * 校验 authorize 路由的 ResponseType 参数
	 */
	public void checkAuthorizeResponseType(String responseType, PkuseRequest req, PkuseOAuth2ServerConfig cfg) {
		// 模式一：Code授权码
		if(responseType.equals(ResponseType.code)) {
			if(!cfg.enableAuthorizationCode) {
				throwErrorSystemNotEnableModel();
			}
			if(!currClientModel().getAllowGrantTypes().contains(GrantType.authorization_code)) {
				throwErrorClientNotEnableModel();
			}
		}
		// 模式二：隐藏式
		else if(responseType.equals(ResponseType.token)) {
			if(!cfg.enableImplicit) {
				throwErrorSystemNotEnableModel();
			}
			if(!currClientModel().getAllowGrantTypes().contains(GrantType.implicit)) {
				throwErrorClientNotEnableModel();
			}
		}
		// 其它
		else {
			throw new PkuseOAuth2Exception("无效 response_type: " + responseType).setCode(PkuseOAuth2ErrorCode.CODE_30125);
		}
	}

	/**
	 * 系统未开放此授权模式时抛出异常
	 */
	public void throwErrorSystemNotEnableModel() {
		throw new PkuseOAuth2Exception("系统暂未开放此授权模式").setCode(PkuseOAuth2ErrorCode.CODE_30141);
	}

	/**
	 * 应用未开放此授权模式时抛出异常
	 */
	public void throwErrorClientNotEnableModel() {
		throw new PkuseOAuth2Exception("应用暂未开放此授权模式").setCode(PkuseOAuth2ErrorCode.CODE_30142);
	}

}
