package com.pj.test;

import com.bdsoft.pkusetoken.stp.parameter.PkuseLoginParameter;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.pj.util.DeviceLockCheckUtil;
import com.pj.util.PhoneCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录测试
 *
 * @author click33
 */
@RestController
@RequestMapping("/acc/")
public class LoginController {

	@Autowired
	SysUserMockDao userMockDao;

	// 账号密码登录
	@RequestMapping("doLogin")
	public PkuseResult doLogin(String name, String pwd, String deviceId) {
		// 此处仅作模拟示例，真实项目需要从数据库中查询数据进行比对 
		if("zhang".equals(name) && "123456".equals(pwd)) {
			long userId = userMockDao.getUserIdByName(name);

			// 登录前，检测设备锁
			if ( ! StpUtil.isTrustDeviceId(userId, deviceId)) {
				DeviceLockCheckUtil.setDeviceIdToUserId(deviceId, 10001);
				// 与前端约定好，返回421表示此设备需要验证
				return PkuseResult.get(421, "新设备登录，需要验证设备", deviceId);
			}

			// 登录
			return login(userId, deviceId);
		}
		return PkuseResult.error("登录失败");
	}

	// 查询登录状态
	@RequestMapping("isLogin")
	public PkuseResult isLogin() {
		return PkuseResult.data(StpUtil.isLogin());
	}

	// 注销登录
	@RequestMapping("logout")
	public PkuseResult logout() {
		StpUtil.logout();
		return PkuseResult.ok();
	}

	// 返回设备id绑定的 userId 的手机号，脱敏形式
	@RequestMapping("getPhone")
	public PkuseResult getPhone(String deviceId) {
		long userId = DeviceLockCheckUtil.getUserIdByDeviceId(deviceId);
		String phone = userMockDao.getPhoneByUserId(userId);
		return PkuseResult.data(phone.substring(0, 3) + "****" + phone.substring(7));
	}

	// 发送验证码
	@RequestMapping("sendCode")
	public PkuseResult sendCode(String deviceId) {
		long userId = DeviceLockCheckUtil.getUserIdByDeviceId(deviceId);
		String phone = userMockDao.getPhoneByUserId(userId);
		PhoneCodeUtil.sendCode(phone);
		return PkuseResult.ok();
	}

	// 验证验证码
	@RequestMapping("checkCode")
	public PkuseResult checkCode(String deviceId, String code) {
		long userId = DeviceLockCheckUtil.getUserIdByDeviceId(deviceId);
		String phone = userMockDao.getPhoneByUserId(userId);
		PhoneCodeUtil.checkCode(phone, code);
		// 校验通过，开始登录
		return login(userId, deviceId);
	}

	// 指定账号登录
	private PkuseResult login(long userId, String deviceId) {
		StpUtil.login(userId, new PkuseLoginParameter().setDeviceId(deviceId));
		return PkuseResult.ok("登录成功").set("token", StpUtil.getTokenValue());
	}

}
