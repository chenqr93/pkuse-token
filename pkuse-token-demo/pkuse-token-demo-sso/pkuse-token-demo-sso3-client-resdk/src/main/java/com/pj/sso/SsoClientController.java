package com.pj.sso;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.spring.SpringMVCUtil;
import com.bdsoft.pkusetoken.sso.message.PkuseSsoMessage;
import com.bdsoft.pkusetoken.sso.processor.PkuseSsoClientProcessor;
import com.bdsoft.pkusetoken.sso.template.PkuseSsoClientTemplate;
import com.bdsoft.pkusetoken.sso.template.PkuseSsoClientUtil;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.pj.resdk.StpLogicForHttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

/**
 * SSO Client端 Controller 
 * @author click33
 */
@RestController
public class SsoClientController {

	// SSO-Client端：首页
	@RequestMapping("/")
	public String index(HttpSession session) {
		boolean isLogin = session.getAttribute("userId") != null;
		Object loginId = session.getAttribute("userId");
		String str = "<h2>Sa-Token SSO-Client 应用端 (模式三-ReSdk)</h2>" +
				"<p>当前会话是否登录：" + isLogin + " (" + loginId + ")</p>" +
				"<p> " +
					"<a href='/sso/login?back=/'>登录</a> - " +
					"<a href='/sso/logoutByAlone?back=/'>单应用注销</a> - " +
					"<a href='/sso/logout?back=self'>全端注销</a> - " +
					"<a href='/sso/myInfo' target='_blank'>账号资料</a>" +
				"</p>";
		return str;
	}

	/*
	 * SSO-Client端：处理所有 SSO 相关请求
	 * 		http://{host}:{port}/sso/login			-- Client 端登录地址
	 * 		http://{host}:{port}/sso/logout			-- Client 端注销地址（isSlo=true时打开）
	 * 		http://{host}:{port}/sso/pushC			-- Client 端接收消息推送地址
	 */
	@RequestMapping("/sso/*")
	public Object ssoLogin() {
		return PkuseSsoClientProcessor.instance.dister();
	}

	// 当前应用独自注销 (不退出其它应用)
	@RequestMapping("/sso/logoutByAlone")
	public Object logoutByAlone(HttpSession session) {
		session.removeAttribute("userId");
		return PkuseSsoClientProcessor.instance._ssoLogoutBack(PkuseHolder.getRequest(), PkuseHolder.getResponse());
	}

	// 配置SSO相关参数
	@Autowired
	private void configSso(PkuseSsoClientTemplate ssoClientTemplate) {

		// 自定义底层使用的会话操作对象
		ssoClientTemplate.setStpLogic(new StpLogicForHttpSession(StpUtil.TYPE));

		// 自定义校验 ticket 返回值的处理逻辑 （每次从认证中心获取校验 ticket 的结果后调用）
		ssoClientTemplate.strategy.ticketResultHandle = (ctr, back) -> {
			HttpSession session = SpringMVCUtil.getRequest().getSession();
			session.setAttribute("userId", ctr.loginId);
			return PkuseHolder.getResponse().redirect(back);
		};
	}

	// 查询我的账号信息：sso-client 前端 -> sso-center 后端 -> sso-server 后端
	@RequestMapping("/sso/myInfo")
	public Object myInfo(HttpSession session) {
		// 如果尚未登录 
		if(session.getAttribute("userId") == null) {
			return "尚未登录，无法获取";
		}

		// 推送消息
		PkuseSsoMessage message = new PkuseSsoMessage();
		message.setType("userinfo");
		message.set("loginId", session.getAttribute("userId"));
		PkuseResult result = PkuseSsoClientUtil.pushMessageAsSaResult(message);

        // 返回给前端 
		return result;
	}

}
