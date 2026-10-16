package com.bdsoft.usercenter.auth.controller;

import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.util.PkuseResult;
import com.bdsoft.usercenter.auth.bo.DeviceCodeBO;
import com.bdsoft.usercenter.auth.bo.LoginResultBO;
import com.bdsoft.usercenter.common.annotation.OperLog;
import com.bdsoft.usercenter.common.config.SystemConfig;
import com.bdsoft.usercenter.auth.dto.ChangePasswordDTO;
import com.bdsoft.usercenter.auth.dto.DeviceCodeDTO;
import com.bdsoft.usercenter.auth.dto.DeviceVerifyDTO;
import com.bdsoft.usercenter.auth.dto.LoginDTO;
import com.bdsoft.usercenter.user.po.UcUser;
import com.bdsoft.usercenter.auth.service.AuthService;
import com.bdsoft.usercenter.auth.service.CaptchaService;
import com.bdsoft.usercenter.auth.service.LoginAttemptService;
import com.bdsoft.usercenter.config.service.UcConfigService;
import com.bdsoft.usercenter.security.Sm2KeyManager;
import com.bdsoft.usercenter.auth.vo.DeviceCodeVO;
import com.bdsoft.usercenter.auth.vo.LoginVO;
import com.bdsoft.usercenter.auth.vo.PasswordRuleVO;
import com.bdsoft.usercenter.user.vo.UserVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录认证接口
 *
 * <pre>
 *   /api/auth/login           账号密码登录（含锁定检查 / 滑块验证码 / 设备锁，详见 AuthService）
 *   /api/auth/captcha         获取滑块验证码（需要时前端调用）
 *   /api/auth/need-captcha    查询某账号当前是否需要滑块验证码
 *   /api/auth/password-rule   当前生效的密码规则（改密页面提示）
 *   /api/auth/change-password 修改当前登录用户的密码
 *   /api/auth/logout          退出登录
 *   /api/auth/me              当前登录用户
 * </pre>
 *
 * <p> 入参为 DTO、出参为 VO：实体不进入接口层。 </p>
 *
 * <p> 登录失败时的业务状态码：421=设备锁拦截（新设备） / 423=账号锁定 / 428=需要滑块验证码（前端据此交互）； </p>
 * <p> 拦截器在「需修改初始密码」状态下返回 426（仅放行改密等必要接口）。 </p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @Resource
    private CaptchaService captchaService;

    @Resource
    private LoginAttemptService loginAttemptService;

    @Resource
    private Sm2KeyManager sm2KeyManager;

    @Resource
    private UcConfigService configService;

    /** 登录 */
    @PostMapping("/login")
    public PkuseResult login(@RequestBody LoginDTO param, HttpServletRequest request) {
        LoginResultBO result = authService.login(param, request);
        return PkuseResult.ok("登录成功").setData(toLoginVO(result));
    }

    /** 获取滑块验证码（返回 id、背景图/滑块图 base64、宽高与随机位置 data） */
    @GetMapping("/captcha")
    public PkuseResult captcha() {
        ImageCaptchaVO captcha = captchaService.generate();
        return PkuseResult.ok().setData(captcha);
    }

    /** 查询账号当前是否需要滑块验证码（连续失败达到阈值后需要） */
    @GetMapping("/need-captcha")
    public PkuseResult needCaptcha(@RequestParam(required = false) String username) {
        return PkuseResult.ok().setData(loginAttemptService.needCaptcha(username));
    }

    /** 获取 SM2 公钥（国密）：前端使用该公钥加密登录密码后提交（encrypted=true） */
    @GetMapping("/public-key")
    public PkuseResult publicKey() {
        return PkuseResult.ok()
                .set("algorithm", "SM2")
                .set("mode", "C1C3C2")
                .setData(sm2KeyManager.getPublicKeyHex());
    }

    /** 新设备登录：向账号绑定手机号发送验证码（参数为登录时返回的 verifyToken） */
    @PostMapping("/device/send-code")
    public PkuseResult sendDeviceCode(@RequestBody DeviceCodeDTO param) {
        DeviceCodeBO result = authService.sendDeviceVerifyCode(param.getVerifyToken());
        DeviceCodeVO data = new DeviceCodeVO();
        data.setPhone(result.getPhone());
        data.setExpireMinutes(result.getExpireMinutes());
        data.setDemoCode(result.getDemoCode());
        return PkuseResult.ok("验证码已发送").setData(data);
    }

    /** 新设备登录：校验验证码并完成换绑登录（解绑原设备、绑定新设备） */
    @PostMapping("/device/verify")
    public PkuseResult verifyDevice(@RequestBody DeviceVerifyDTO param, HttpServletRequest request) {
        LoginResultBO result = authService.verifyDeviceAndLogin(param.getVerifyToken(), param.getCode(), request);
        return PkuseResult.ok("验证通过，已绑定新设备").setData(toLoginVO(result));
    }

    /** 当前生效的密码规则（登录用户可读：改密页面提示；不含系统默认密码） */
    @GetMapping("/password-rule")
    public PkuseResult passwordRule() {
        SystemConfig config = configService.get();
        PasswordRuleVO data = PasswordRuleVO.from(config);
        // 登录用户可见接口不下发系统默认密码（仅管理端 /api/config/password-rule 返回）
        data.setDefaultPassword(null);
        return PkuseResult.ok().setData(data);
    }

    /** 修改当前登录用户的密码（原密码 + 新密码，支持 SM2 密文；成功后清除「需修改密码」标记） */
    @OperLog(module = "账号安全", action = "修改密码")
    @PostMapping("/change-password")
    public PkuseResult changePassword(@RequestBody ChangePasswordDTO param) {
        authService.changePassword(String.valueOf(StpUtil.getLoginId()), param.getOldPassword(), param.getNewPassword(),
                Boolean.TRUE.equals(param.getEncrypted()));
        return PkuseResult.ok("密码修改成功");
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public PkuseResult logout(HttpServletRequest request) {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        String username = null;
        if (loginId != null) {
            UcUser user = authService.currentUser();
            username = user.getUsername();
        }
        String deviceId = request.getParameter("deviceId");
        StpUtil.logout();
        if (username != null) {
            authService.recordLogout(username, loginId.toString(), deviceId, request);
        }
        return PkuseResult.ok("已退出登录");
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public PkuseResult me() {
        return PkuseResult.ok().setData(UserVO.from(authService.currentUser()));
    }

    /** 登录结果 → 视图对象（token 值前端存 localStorage，请求时放入 satoken 请求头） */
    private static LoginVO toLoginVO(LoginResultBO result) {
        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setUser(UserVO.from(result.getUser()));
        vo.setMustChangePassword(result.getMustChangePassword());
        return vo;
    }

}
