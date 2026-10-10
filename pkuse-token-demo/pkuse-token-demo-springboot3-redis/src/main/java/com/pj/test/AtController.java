package com.pj.test;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bdsoft.pkusetoken.annotation.PkuseCheckHttpBasic;
import com.bdsoft.pkusetoken.annotation.PkuseCheckLogin;
import com.bdsoft.pkusetoken.annotation.PkuseCheckPermission;
import com.bdsoft.pkusetoken.annotation.PkuseCheckRole;
import com.bdsoft.pkusetoken.annotation.PkuseCheckSafe;
import com.bdsoft.pkusetoken.annotation.PkuseMode;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;

/**
 * 注解鉴权测试 
 * @author click33
 *
 */
@RestController
@RequestMapping("/at/")
public class AtController {

	// 登录认证，登录之后才可以进入方法  ---- http://localhost:8081/at/checkLogin 
	@PkuseCheckLogin
	@RequestMapping("checkLogin")
	public PkuseResult checkLogin() {
		return PkuseResult.ok();
	}
	
	// 权限认证，具备user-add权限才可以进入方法  ---- http://localhost:8081/at/checkPermission 
	@PkuseCheckPermission("user-add")
	@RequestMapping("checkPermission")
	public PkuseResult checkPermission() {
		return PkuseResult.ok();
	}

	// 权限认证，同时具备所有权限才可以进入  ---- http://localhost:8081/at/checkPermissionAnd 
	@PkuseCheckPermission({"user-add", "user-delete", "user-update"})
	@RequestMapping("checkPermissionAnd")
	public PkuseResult checkPermissionAnd() {
		return PkuseResult.ok();
	}

	// 权限认证，只要具备其中一个就可以进入  ---- http://localhost:8081/at/checkPermissionOr 
	@PkuseCheckPermission(value = {"user-add", "user-delete", "user-update"}, mode = PkuseMode.OR)
	@RequestMapping("checkPermissionOr")
	public PkuseResult checkPermissionOr() {
		return PkuseResult.ok();
	}

	// 角色认证，只有具备admin角色才可以进入  ---- http://localhost:8081/at/checkRole 
	@PkuseCheckRole("admin")
	@RequestMapping("checkRole")
	public PkuseResult checkRole() {
		return PkuseResult.ok();
	}

	// 完成二级认证  ---- http://localhost:8081/at/openSafe 
	@RequestMapping("openSafe")
	public PkuseResult openSafe() {
		StpUtil.openSafe(200); // 打开二级认证，有效期为200秒
		return PkuseResult.ok();
	}
	
	// 通过二级认证后才可以进入  ---- http://localhost:8081/at/checkSafe 
	@PkuseCheckSafe
	@RequestMapping("checkSafe")
	public PkuseResult checkSafe() {
		return PkuseResult.ok();
	}
	
	// 通过Basic认证后才可以进入  ---- http://localhost:8081/at/checkBasic 
	@PkuseCheckHttpBasic(account = "sa:123456")
	@RequestMapping("checkBasic")
	public PkuseResult checkBasic() {
		return PkuseResult.ok();
	}
	
}
