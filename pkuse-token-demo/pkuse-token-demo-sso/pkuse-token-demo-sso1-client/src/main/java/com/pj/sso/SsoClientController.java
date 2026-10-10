package com.pj.sso;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoClientConfig;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Sa-Token-SSO Client端 Controller 
 * @author click33
 */
@RestController
public class SsoClientController {

	// SSO-Client端：首页 
	@RequestMapping("/")
	public String index(HttpServletRequest request) {
		String url = PkuseFoxUtil.encodeUrl( PkuseFoxUtil.joinParam(PkuseHolder.getRequest().getUrl(), request.getQueryString()) );
		PkuseSsoClientConfig cfg = PkuseSsoManager.getClientConfig();

		String str = "<h2>Sa-Token SSO-Client 应用端 (模式一)</h2>" +
					"<p>当前会话是否登录：" + StpUtil.isLogin() + " (" + StpUtil.getLoginId("") + ")</p>" +
					"<p>" +
						"<a href='" + cfg.splicingAuthUrl() + "?mode=simple&client=" + cfg.getClient() + "&redirect=" + url + "'>登录</a> - " +
						"<a href='" + cfg.splicingSignoutUrl() + "?singleDeviceIdLogout=true&back=" + url + "'>单浏览器注销</a> - " +
						"<a href='" + cfg.splicingSignoutUrl() + "?back=" + url + "'>全端注销</a> " +
					"</p>";
		return str;
	}
	
	// 全局异常拦截 
	@ExceptionHandler
	public PkuseResult handlerException(Exception e) {
		e.printStackTrace(); 
		return PkuseResult.error(e.getMessage());
	}
	
}
