package com.pj.h5;

import com.bdsoft.pkusetoken.sso.model.PkuseCheckTicketResult;
import com.bdsoft.pkusetoken.sso.processor.PkuseSsoClientProcessor;
import com.bdsoft.pkusetoken.sso.template.PkuseSsoClientUtil;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLoginParameter;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前后台分离架构下集成SSO所需的代码 （SSO-Client端）
 * <p>（注：如果不需要前后端分离架构下集成SSO，可删除此包下所有代码）</p>
 *
 * @author click33
 */
@RestController
public class H5Controller {

	// 当前是否登录 
	@RequestMapping("/sso/isLogin")
	public Object isLogin() {
		return PkuseResult.data(StpUtil.isLogin()).set("loginId", StpUtil.getLoginIdDefaultNull());
	}
	
	// 返回SSO认证中心登录地址 
	@RequestMapping("/sso/getSsoAuthUrl")
	public PkuseResult getSsoAuthUrl(String clientLoginUrl) {
		String serverAuthUrl = PkuseSsoClientUtil.buildServerAuthUrl(clientLoginUrl, "");
		return PkuseResult.data(serverAuthUrl);
	}
	
	// 根据ticket进行登录
	@RequestMapping("/sso/doLoginByTicket")
	public PkuseResult doLoginByTicket(String ticket) {
		PkuseCheckTicketResult ctr = PkuseSsoClientProcessor.instance.checkTicket(ticket, "/sso/doLoginByTicket");
		StpUtil.login(ctr.loginId, new PkuseLoginParameter()
				.setTimeout(ctr.remainTokenTimeout)
				.setDeviceId(ctr.deviceId)
		);
		return PkuseResult.data(StpUtil.getTokenValue());
	}

}
