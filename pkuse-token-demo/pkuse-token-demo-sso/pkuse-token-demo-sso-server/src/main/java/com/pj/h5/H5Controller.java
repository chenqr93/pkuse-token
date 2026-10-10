package com.pj.h5;

import com.bdsoft.pkusetoken.sso.template.PkuseSsoServerUtil;
import com.bdsoft.pkusetoken.sso.util.PkuseSsoConsts;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前后台分离架构下集成SSO所需的代码 （SSO-Server端）
 * <p>（注：如果不需要前后端分离架构下集成SSO，可删除此包下所有代码）</p>
 * @author click33
 *
 */
@RestController
public class H5Controller {

	/**
	 * 返回当前是否已经登录
	 */
	@RequestMapping("/sso/isLogin")
	public PkuseResult isLogin() {
		return PkuseResult.data(StpUtil.isLogin());
	}

	/**
	 * 获取 redirectUrl 
	 */
	@RequestMapping("/sso/getRedirectUrl")
	public PkuseResult getRedirectUrl(String client, String redirect, String mode) {
		// 未登录情况下，返回 code=401 
		if(StpUtil.isLogin() == false) {
			return PkuseResult.code(401);
		}
		// 已登录情况下，构建 redirectUrl
		redirect = PkuseFoxUtil.decoderUrl(redirect);
		if(PkuseSsoConsts.MODE_SIMPLE.equals(mode)) {
			// 模式一 
			PkuseSsoServerUtil.checkRedirectUrl(client, redirect);
			return PkuseResult.data(redirect);
		} else {
			// 模式二或模式三
			String redirectUrl = PkuseSsoServerUtil.buildRedirectUrl(client, redirect, StpUtil.getLoginId(), StpUtil.getTokenValue());
			return PkuseResult.data(redirectUrl);
		}
	}

}
