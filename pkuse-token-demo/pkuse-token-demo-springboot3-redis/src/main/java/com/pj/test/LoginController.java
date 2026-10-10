package com.pj.test;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;

/**
 * 登录测试 
 * @author click33
 *
 */
@RestController
@RequestMapping("/acc/")
public class LoginController {

	// 测试登录  ---- http://localhost:8081/acc/doLogin?name=zhang&pwd=123456
	@RequestMapping("doLogin")
	public PkuseResult doLogin(String name, String pwd) {
		// 此处仅作模拟示例，真实项目需要从数据库中查询数据进行比对 
		if("zhang".equals(name) && "123456".equals(pwd)) {
			StpUtil.login(10001);
			return PkuseResult.ok("登录成功");
		}
		return PkuseResult.error("登录失败");
	}

	// 查询登录状态  ---- http://localhost:8081/acc/isLogin
	@RequestMapping("isLogin")
	public PkuseResult isLogin() {
		return PkuseResult.ok("是否登录：" + StpUtil.isLogin());
	}

	// 查询 Token 信息  ---- http://localhost:8081/acc/tokenInfo
	@RequestMapping("tokenInfo")
	public PkuseResult tokenInfo() {
		return PkuseResult.data(StpUtil.getTokenInfo());
	}
	
	// 测试注销  ---- http://localhost:8081/acc/logout
	@RequestMapping("logout")
	public PkuseResult logout() {
		StpUtil.logout();
		return PkuseResult.ok();
	}
	
}
