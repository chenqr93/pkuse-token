package com.bdsoft.usercenter.auth.service;

import com.bdsoft.usercenter.auth.bo.DeviceCodeBO;
import com.bdsoft.usercenter.auth.bo.LoginResultBO;
import com.bdsoft.usercenter.auth.dto.LoginDTO;
import com.bdsoft.usercenter.user.po.UcUser;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录认证 Service
 *
 * <p> 统一登录（用户中心自身登录 + SSO-Server 的 doLogin 处理）共用此处的校验逻辑， </p>
 * <p> 完整流程包含：账号锁定检查 → 滑块验证码校验 → 密码校验（国密 SM3 摘要，支持 SM2 密文传输） </p>
 * <p> → 设备锁校验（首次登录绑定设备指纹；更换设备需短信验证码验证后换绑） → 登录并记录日志。 </p>
 * <p> 涉及策略（验证码 / 设备锁 / 锁定阈值 / 默认密码 / 密码复杂度）均取自「规则配置」（uc_config），修改后即时生效。 </p>
 *
 * <p> 入参为 DTO、出参为 BO / 实体，均在 Controller 与 Service 层之间流转，不直接返回前端。 </p>
 */
public interface AuthService {

    /** 业务状态码：需先修改初始密码（规则配置 - 首次登录强制修改密码） */
    int CODE_MUST_CHANGE_PASSWORD = 426;

    /**
     * 校验账号密码并完成登录（按登录安全策略校验滑块验证码）
     *
     * @param param   登录参数（账号、密码或密码密文、设备指纹、验证码）
     * @param request 当前请求（用于记录 IP / UA）
     * @return 登录结果（登录用户 + 是否需修改初始密码）
     */
    default LoginResultBO login(LoginDTO param, HttpServletRequest request) {
        return login(param, request, true);
    }

    /**
     * 校验账号密码并完成登录（含登录安全策略：锁定 / 验证码 / 设备锁 / 登录日志）
     *
     * @param param          登录参数
     * @param request        当前请求
     * @param requireCaptcha 是否按策略校验滑块验证码
     *                       （SSO 协议登录接口 /sso/doLogin 传 false：该接口由应用签名/票据保护，不面向人工登录页）
     * @return 登录结果（登录用户 + 是否需修改初始密码）
     */
    LoginResultBO login(LoginDTO param, HttpServletRequest request, boolean requireCaptcha);

    /**
     * 新设备登录：发送验证码（发送到账号绑定手机号）
     *
     * @param verifyToken 登录时（新设备）返回的验证凭证
     * @return 验证码发送结果（脱敏手机号、有效期；演示模式同时含验证码）
     */
    DeviceCodeBO sendDeviceVerifyCode(String verifyToken);

    /**
     * 新设备登录：校验验证码并完成换绑登录（解绑原设备 → 绑定新设备 → 登录）
     *
     * @param verifyToken 验证凭证
     * @param code        短信验证码
     * @param request     当前请求
     * @return 登录结果（登录用户 + 是否需修改初始密码）
     */
    LoginResultBO verifyDeviceAndLogin(String verifyToken, String code, HttpServletRequest request);

    /** 当前登录用户 ID（未登录时抛出 NotLoginException） */
    String currentLoginId();

    /** 当前登录用户（数据库实体，不存在时抛出业务异常） */
    UcUser currentUser();

    /**
     * 修改当前登录用户的密码
     *
     * <p> 校验原密码 → 按「规则配置 - 密码规则」校验新密码 → 更新密码摘要并清除「需修改密码」标记。 </p>
     *
     * @param userId      用户 ID
     * @param oldPassword 原密码（明文或 SM2 密文）
     * @param newPassword 新密码（明文或 SM2 密文）
     * @param encrypted   原密码/新密码是否为 SM2 密文
     */
    void changePassword(String userId, String oldPassword, String newPassword, boolean encrypted);

    /**
     * 当前账号是否处于「需修改密码」状态（规则配置：首次登录强制修改密码）
     *
     * <p> 用于接口拦截：处于该状态时，除修改密码等必要接口外均被拒绝（code=426）。 </p>
     */
    boolean isPasswordChangeRequired(String userId);

    /** 记录注销日志 */
    void recordLogout(String username, String userId, String deviceId, HttpServletRequest request);

}
