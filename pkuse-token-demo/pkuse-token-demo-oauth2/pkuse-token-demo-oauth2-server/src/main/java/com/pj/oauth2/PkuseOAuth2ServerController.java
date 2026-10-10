package com.pj.oauth2;

import com.bdsoft.pkusetoken.context.PkuseHolder;
import com.bdsoft.pkusetoken.oauth2.config.PkuseOAuth2ServerConfig;
import com.bdsoft.pkusetoken.oauth2.processor.PkuseOAuth2ServerProcessor;
import com.bdsoft.pkusetoken.oauth2.strategy.PkuseOAuth2Strategy;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * Sa-Token-OAuth2 Server 认证端 Controller
 *
 * @author click33
 */
@RestController
public class PkuseOAuth2ServerController {

	// OAuth2-Server 端：处理所有 OAuth2 相关请求
	@RequestMapping("/oauth2/*")
	public Object request() {
		System.out.println("------- 进入请求: " + PkuseHolder.getRequest().getUrl());
		return PkuseOAuth2ServerProcessor.instance.dister();
	}

	// Sa-Token OAuth2 定制化配置
	@Autowired
	public void configOAuth2Server(PkuseOAuth2ServerConfig oauth2Server) {
		// 未登录的视图
		PkuseOAuth2Strategy.instance.notLoginView = ()->{
			return new ModelAndView("login.html");
		};

		// 登录处理函数
		PkuseOAuth2Strategy.instance.doLoginHandle = (name, pwd) -> {
			if("sa".equals(name) && "123456".equals(pwd)) {
				StpUtil.login(10001);
				return PkuseResult.ok().set("satoken", StpUtil.getTokenValue());
			}
			return PkuseResult.error("账号名或密码错误");
		};

		// 授权确认视图
		PkuseOAuth2Strategy.instance.confirmView = (clientId, scopes)->{
			Map<String, Object> map = new HashMap<>();
			map.put("clientId", clientId);
			map.put("scope", scopes);
			return new ModelAndView("confirm.html", map);
		};

	}

}
