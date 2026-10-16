package com.bdsoft.usercenter.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.pkusetoken.stp.StpUtil;
import com.bdsoft.pkusetoken.stp.parameter.PkuseLoginParameter;
import com.bdsoft.pkusetoken.stp.parameter.enums.PkuseReplacedLoginExitMode;
import com.bdsoft.pkusetoken.stp.parameter.enums.PkuseReplacedRange;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bdsoft.usercenter.auth.bo.DeviceCodeBO;
import com.bdsoft.usercenter.auth.bo.DeviceVerifyBO;
import com.bdsoft.usercenter.auth.bo.LoginResultBO;
import com.bdsoft.usercenter.common.exception.BizException;
import com.bdsoft.usercenter.common.util.DeviceTypes;
import com.bdsoft.usercenter.common.util.PasswordUtil;
import com.bdsoft.usercenter.common.util.RequestInfoUtil;
import com.bdsoft.usercenter.common.config.LoginSecurityProperties;
import com.bdsoft.usercenter.common.config.SystemConfig;
import com.bdsoft.usercenter.auth.dto.LoginDTO;
import com.bdsoft.usercenter.log.po.UcLoginLog;
import com.bdsoft.usercenter.user.po.UcUser;
import com.bdsoft.usercenter.user.mapper.UcUserMapper;
import com.bdsoft.usercenter.security.Sm2KeyManager;
import com.bdsoft.usercenter.auth.service.AuthService;
import com.bdsoft.usercenter.auth.service.CaptchaService;
import com.bdsoft.usercenter.auth.service.LoginAttemptService;
import com.bdsoft.usercenter.log.service.LoginLogService;
import com.bdsoft.usercenter.config.service.UcConfigService;
import com.bdsoft.usercenter.device.service.UcUserDeviceService;
import com.bdsoft.usercenter.user.service.UcUserService;
import com.bdsoft.usercenter.auth.vo.DeviceNeedVerifyVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 登录认证 Service 实现
 *
 * <p> 登录安全流程： </p>
 * <ol>
 *   <li>密码传输加密：前端使用 SM2 公钥加密密码（国密），服务端私钥解密；</li>
 *   <li>账号锁定检查：锁定期内直接拒绝（可由管理员在线解锁）；</li>
 *   <li>滑块验证码校验：连续失败达到策略阈值后强制校验（tianai-captcha）；</li>
 *   <li>密码校验：SM3(salt + 明文) 比对（历史 MD5 数据校验通过后自动升级为 SM3），
 *       失败累计计数，达到阈值锁定账号；</li>
 *   <li>设备锁校验：首次登录绑定设备指纹；已绑定设备直接放行；
 *       更换设备时返回 421 并下发验证凭证，需短信验证码验证后解绑原设备、绑定新设备（可在「规则配置」中关闭）；</li>
 *   <li>登录成功：清零失败计数、更新最后登录时间与设备信息；</li>
 *   <li>全流程写入登录日志（分区频率可在「规则配置」中调整）；</li>
 *   <li>「需修改初始密码」标记（规则配置 - 首次登录强制修改密码）随登录态写入 Redis，
 *       由拦截器在修改密码前阻断其他接口（code=426）。 </li>
 * </ol>
 */
@Service
public class AuthServiceImpl extends ServiceImpl<UcUserMapper, UcUser> implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** 业务状态码：新设备登录（需验证） */
    public static final int CODE_DEVICE_VERIFY = 421;

    /** 业务状态码：账号锁定 */
    public static final int CODE_LOCKED = 423;

    /** 业务状态码：需要滑块验证码 */
    public static final int CODE_NEED_CAPTCHA = 428;

    /** 设备验证凭证在 Redis 中的 key 前缀 */
    private static final String VERIFY_TOKEN_KEY = "uc:device:verify:";

    /** 手机验证码在 Redis 中的 key 前缀 */
    private static final String PHONE_CODE_KEY = "uc:device:code:";

    /** 「需修改密码」标记在 Redis 中的 key 前缀（登录时写入，修改密码后清除） */
    private static final String MUST_CHANGE_PASSWORD_KEY = "uc:pwd:must-change:";

    /** JSON 序列化（验证凭证存储使用） */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private LoginSecurityProperties properties;

    @Resource
    private UcConfigService configService;

    @Resource
    private LoginAttemptService loginAttemptService;

    @Resource
    private CaptchaService captchaService;

    @Resource
    private LoginLogService loginLogService;

    @Resource
    private UcUserDeviceService userDeviceService;

    @Resource
    private Sm2KeyManager sm2KeyManager;

    @Override
    public LoginResultBO login(LoginDTO param, HttpServletRequest request, boolean requireCaptcha) {
        String username = param.getUsername();
        if (username == null || username.isBlank() || param.getPassword() == null || param.getPassword().isEmpty()) {
            throw new BizException("账号或密码不能为空");
        }
        // 密码：国密 SM2 密文则先解密（前端使用公钥加密后传输）
        String password = Boolean.TRUE.equals(param.getEncrypted())
                ? decryptPassword(param.getPassword())
                : param.getPassword();

        // 设备指纹：前端未上报时按浏览器 User-Agent 生成（保证 SSO / 接口调用等场景同样受设备锁保护）
        String deviceId = resolveDeviceId(param.getDeviceId(), request);
        String ip = RequestInfoUtil.getClientIp(request);
        String userAgent = RequestInfoUtil.getUserAgent(request);
        // 设备类型：前端显式上报优先（pc / mobile / mini），未上报时按 User-Agent 推断
        String deviceType = DeviceTypes.resolve(param.getDeviceType(), userAgent);

        UcUser user = this.getOne(new LambdaQueryWrapper<UcUser>().eq(UcUser::getUsername, username));
        if (user == null) {
            // 不暴露账号是否存在，但同样计入失败次数（防止账号枚举与暴力尝试）
            loginAttemptService.increaseFailCount(username);
            loginLogService.record(username, null, "login", UcLoginLog.RESULT_NO_USER,
                    "账号不存在", deviceId, request);
            throw new BizException("账号或密码错误");
        }

        LocalDateTime now = LocalDateTime.now();

        // 1、账号锁定检查
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long remainMinutes = Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes() + 1);
            String msg = "账号锁定中，剩余约 " + remainMinutes + " 分钟";
            loginLogService.record(username, user.getId(), "login", UcLoginLog.RESULT_LOCKED, msg, deviceId, request);
            throw new BizException(CODE_LOCKED, "账号已锁定（" + user.getLockReason() + "），"
                    + "剩余约 " + remainMinutes + " 分钟，可联系管理员在线解锁");
        }

        // 2、滑块验证码校验（策略：captcha-after-fail=0 时默认每次登录都需要）
        if (requireCaptcha && loginAttemptService.needCaptcha(username)) {
            boolean captchaOk = captchaService.verify(param.getCaptchaId(), param.getCaptchaTrack());
            if (!captchaOk) {
                loginLogService.record(username, user.getId(), "login", UcLoginLog.RESULT_CAPTCHA_FAIL,
                        "滑块验证码校验不通过", deviceId, request);
                throw new BizException(CODE_NEED_CAPTCHA, "请完成滑块验证");
            }
        }

        // 3、密码校验（国密 SM3 + 盐；历史 MD5 数据校验通过后自动升级）
        if (!PasswordUtil.matches(password, user.getPassword(), user.getPasswordSalt())) {
            handlePasswordFail(user, username, deviceId, request, now);
            // handlePasswordFail 内部抛出异常
            throw new BizException("账号或密码错误");
        }
        upgradePasswordIfNeeded(user, password);

        // 4、账号状态检查
        if (!Integer.valueOf(UcUser.STATUS_ENABLED).equals(user.getStatus())) {
            loginLogService.record(username, user.getId(), "login", UcLoginLog.RESULT_DISABLED,
                    "账号已被禁用", deviceId, request);
            throw new BizException("该账号已被禁用，请联系管理员");
        }

        // 5、设备锁校验（可在「规则配置」中开关；首次登录绑定设备指纹；更换设备需验证后换绑）
        if (configService.get().isDeviceVerifyEnabled() && !isExemptDeviceLock(user)) {
            if (userDeviceService.isBound(user.getId(), deviceId)) {
                userDeviceService.touch(user.getId(), deviceId, ip, userAgent);
            } else if (userDeviceService.countByUserId(user.getId()) == 0) {
                // 首次登录：自动绑定当前设备指纹
                userDeviceService.bind(user.getId(), deviceId, ip, userAgent);
                log.info("账号 {} 首次登录，已绑定设备指纹：{}", username, abbreviate(deviceId));
            } else {
                // 更换设备：返回 421 与验证凭证，前端发起短信验证码验证
                throw newDeviceVerifyException(user, deviceId, deviceType, ip, userAgent, request);
            }
        }

        // 6、登录并记录日志（需修改初始密码时在日志中标注）
        doLogin(user, deviceId, deviceType, now);
        loginLogService.record(username, user.getId(), "login", UcLoginLog.RESULT_SUCCESS,
                needChangePassword(user) ? "登录成功（需修改初始密码）" : "登录成功", deviceId, request);

        user.setPassword(null);
        return new LoginResultBO(user, needChangePassword(user));
    }

    @Override
    public DeviceCodeBO sendDeviceVerifyCode(String verifyToken) {
        DeviceVerifyBO info = getVerifyInfoOrThrow(verifyToken);
        UcUser user = this.getById(info.getUserId());
        if (user == null) {
            throw new BizException("验证已失效，请重新登录");
        }
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new BizException("账号未绑定手机号，无法自助验证，请联系管理员解绑设备");
        }
        // 生成 6 位验证码并保存（有效期见配置）
        String code = String.valueOf(PkuseFoxUtil.getRandomNumber(100000, 999999));
        PkuseManager.getSaTokenDao().set(PHONE_CODE_KEY + user.getId(), code,
                properties.getDeviceVerifyMinutes() * 60L);

        String maskedPhone = maskPhone(user.getPhone());
        // 验证码属敏感信息：仅演示模式（无短信通道）允许随日志输出，便于自测
        if (properties.isDeviceVerifyShowCode()) {
            log.info("新设备登录验证码已发送（演示模式）：账号={}，手机号={}，验证码={}（有效期 {} 分钟）",
                    user.getUsername(), maskedPhone, code, properties.getDeviceVerifyMinutes());
        } else {
            log.info("新设备登录验证码已发送：账号={}，手机号={}（有效期 {} 分钟）",
                    user.getUsername(), maskedPhone, properties.getDeviceVerifyMinutes());
        }
        loginLogService.record(user.getUsername(), user.getId(), "login", UcLoginLog.RESULT_DEVICE_VERIFY,
                "更换设备登录，已发送验证码至 " + maskedPhone, info.getDeviceId(), null);

        // 演示模式（无短信通道）：验证码随结果返回给前端展示（生产环境必须关闭）
        return new DeviceCodeBO(maskedPhone, properties.getDeviceVerifyMinutes(),
                properties.isDeviceVerifyShowCode() ? code : null);
    }

    @Override
    public LoginResultBO verifyDeviceAndLogin(String verifyToken, String code, HttpServletRequest request) {
        DeviceVerifyBO info = getVerifyInfoOrThrow(verifyToken);
        UcUser user = this.getById(info.getUserId());
        if (user == null) {
            throw new BizException("验证已失效，请重新登录");
        }
        // 校验短信验证码
        String cachedCode = PkuseManager.getSaTokenDao().get(PHONE_CODE_KEY + user.getId());
        if (cachedCode == null) {
            throw new BizException("验证码已过期，请重新发送");
        }
        if (code == null || !cachedCode.equals(code.trim())) {
            loginLogService.record(user.getUsername(), user.getId(), "login", UcLoginLog.RESULT_DEVICE_VERIFY,
                    "新设备验证码错误", info.getDeviceId(), request);
            throw new BizException("验证码错误");
        }
        PkuseManager.getSaTokenDao().delete(PHONE_CODE_KEY + user.getId());
        PkuseManager.getSaTokenDao().deleteObject(VERIFY_TOKEN_KEY + verifyToken);

        // 解绑原设备、绑定新设备（设备指纹换绑）
        userDeviceService.replaceDevices(user.getId(), info.getDeviceId(), info.getIp(), info.getUserAgent());

        // 登录并记录日志（需修改初始密码时在日志中标注）
        LocalDateTime now = LocalDateTime.now();
        doLogin(user, info.getDeviceId(), info.getDeviceType(), now);
        loginLogService.record(user.getUsername(), user.getId(), "login", UcLoginLog.RESULT_DEVICE_REBIND,
                "更换设备验证通过：已解绑原设备并绑定新设备（" + abbreviate(info.getDeviceId()) + "）",
                info.getDeviceId(), request);

        user.setPassword(null);
        return new LoginResultBO(user, needChangePassword(user));
    }

    @Override
    public String currentLoginId() {
        return String.valueOf(StpUtil.getLoginId());
    }

    @Override
    public UcUser currentUser() {
        UcUser user = this.getById(String.valueOf(StpUtil.getLoginId()));
        if (user == null) {
            throw new BizException("登录用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public void changePassword(String userId, String oldPassword, String newPassword, boolean encrypted) {
        UcUser user = this.getById(userId);
        if (user == null) {
            throw new BizException("登录用户不存在");
        }
        String oldRaw = encrypted ? decryptPassword(oldPassword) : oldPassword;
        String newRaw = encrypted ? decryptPassword(newPassword) : newPassword;
        if (oldRaw == null || oldRaw.isEmpty() || newRaw == null || newRaw.isEmpty()) {
            throw new BizException("原密码与新密码均不能为空");
        }
        if (!PasswordUtil.matches(oldRaw, user.getPassword(), user.getPasswordSalt())) {
            throw new BizException("原密码不正确");
        }
        if (oldRaw.equals(newRaw)) {
            throw new BizException("新密码不能与原密码相同");
        }
        // 密码复杂度校验（规则配置 - 密码规则）
        String passwordError = configService.get().checkPassword(newRaw);
        if (passwordError != null) {
            throw new BizException(passwordError);
        }
        String salt = PasswordUtil.newSalt();
        this.update(new LambdaUpdateWrapper<UcUser>()
                .eq(UcUser::getId, userId)
                .set(UcUser::getPassword, PasswordUtil.encode(newRaw, salt))
                .set(UcUser::getPasswordSalt, salt)
                .set(UcUser::getMustChangePassword, UcUser.MUST_CHANGE_PASSWORD_NO)
                .set(UcUser::getUpdateTime, LocalDateTime.now()));
        clearMustChangePasswordFlag(userId);
        log.info("账号 {} 修改密码成功", user.getUsername());
    }

    @Override
    public boolean isPasswordChangeRequired(String userId) {
        if (userId == null) {
            return false;
        }
        return PkuseManager.getSaTokenDao().get(MUST_CHANGE_PASSWORD_KEY + userId) != null;
    }

    /** 记录注销日志 */
    @Override
    public void recordLogout(String username, String userId, String deviceId, HttpServletRequest request) {
        loginLogService.record(username, userId, "logout", UcLoginLog.RESULT_SUCCESS, "退出登录",
                deviceId, request);
    }

    // ------------------------- 私有方法

    /** 密码错误：累计次数，达到阈值锁定账号（阈值与锁定时长来自「规则配置」） */
    private void handlePasswordFail(UcUser user, String username, String deviceId,
                                    HttpServletRequest request, LocalDateTime now) {
        SystemConfig config = configService.get();
        long failCount = loginAttemptService.increaseFailCount(username);
        int maxFailCount = config.getMaxFailCount();
        if (failCount >= maxFailCount) {
            LocalDateTime lockedUntil = now.plusMinutes(config.getLockMinutes());
            this.update(new LambdaUpdateWrapper<UcUser>()
                    .eq(UcUser::getId, user.getId())
                    .set(UcUser::getLoginFailCount, (int) failCount)
                    .set(UcUser::getLockedUntil, lockedUntil)
                    .set(UcUser::getLockReason, "连续密码错误 " + failCount + " 次")
                    .set(UcUser::getUpdateTime, now));
            loginAttemptService.clearFailCount(username);
            String msg = "连续密码错误 " + failCount + " 次，账号锁定至 " + lockedUntil.format(TIME_FORMATTER);
            loginLogService.record(username, user.getId(), "login", UcLoginLog.RESULT_LOCKED, msg, deviceId, request);
            throw new BizException(CODE_LOCKED, "密码连续错误 " + failCount + " 次，账号已锁定 "
                    + config.getLockMinutes() + " 分钟，可联系管理员在线解锁");
        }
        String msg = "密码错误（连续失败 " + failCount + " 次）";
        loginLogService.record(username, user.getId(), "login", UcLoginLog.RESULT_FAIL, msg, deviceId, request);
        throw new BizException("账号或密码错误（还可尝试 " + (maxFailCount - failCount) + " 次）");
    }

    /** 历史 MD5 密码：校验通过后升级为国密 SM3 + 随机盐 */
    private void upgradePasswordIfNeeded(UcUser user, String rawPassword) {
        if (!PasswordUtil.needUpgrade(user.getPassword(), user.getPasswordSalt())) {
            return;
        }
        String salt = PasswordUtil.newSalt();
        this.update(new LambdaUpdateWrapper<UcUser>()
                .eq(UcUser::getId, user.getId())
                .set(UcUser::getPassword, PasswordUtil.encode(rawPassword, salt))
                .set(UcUser::getPasswordSalt, salt)
                .set(UcUser::getUpdateTime, LocalDateTime.now()));
        user.setPasswordSalt(salt);
        log.info("账号 {} 的密码摘要已从 MD5 升级为国密 SM3", user.getUsername());
    }

    /**
     * 完成登录：写入会话、清零失败计数、更新最后登录时间、写入「需修改密码」标记
     *
     * <p> 设备类型随会话写入终端信息（pc / mobile / mini）； </p>
     * <p> 规则配置「踢掉同类型设备在线账号」开启时，新登录会顶掉同设备类型的已在线会话（其它类型不受影响）。 </p>
     */
    private void doLogin(UcUser user, String deviceId, String deviceType, LocalDateTime now) {
        String normalizedDeviceType = DeviceTypes.normalize(deviceType);
        PkuseLoginParameter loginParameter = new PkuseLoginParameter()
                .setDeviceId(deviceId)
                .setDeviceType(normalizedDeviceType.isEmpty() ? null : normalizedDeviceType);
        if (configService.get().isKickSameDeviceType()) {
            // 同类型设备互踢：新登录成功后旧的同类型设备会话下线；PC / 移动端 / 小程序 之间互不影响
            loginParameter.setIsConcurrent(false)
                    .setReplacedRange(PkuseReplacedRange.CURR_DEVICE_TYPE)
                    .setReplacedLoginExitMode(PkuseReplacedLoginExitMode.OLD_DEVICE);
        }
        StpUtil.login(user.getId(), loginParameter);
        loginAttemptService.clearFailCount(user.getUsername());
        this.update(new LambdaUpdateWrapper<UcUser>()
                .eq(UcUser::getId, user.getId())
                .set(UcUser::getLastLoginTime, now)
                .set(UcUser::getUpdateTime, now)
                .set(UcUser::getLoginFailCount, 0)
                .set(UcUser::getLockedUntil, null)
                .set(UcUser::getLockReason, null));
        user.setLastLoginTime(now);
        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        user.setLockReason(null);

        // 「需修改初始密码」标记：写入 Redis（随登录态有效期），修改密码后清除
        if (needChangePassword(user)) {
            PkuseManager.getSaTokenDao().set(MUST_CHANGE_PASSWORD_KEY + user.getId(), "1",
                    StpUtil.getTokenTimeout());
            log.info("账号 {} 需修改初始密码（规则配置 - 首次登录强制修改密码）", user.getUsername());
        }
    }

    /** 是否处于「需修改密码」状态（规则配置 - 首次登录强制修改密码） */
    private static boolean needChangePassword(UcUser user) {
        return user.getMustChangePassword() != null
                && user.getMustChangePassword() == UcUser.MUST_CHANGE_PASSWORD_YES;
    }

    /** 清除「需修改密码」标记（Redis） */
    private void clearMustChangePasswordFlag(String userId) {
        PkuseManager.getSaTokenDao().delete(MUST_CHANGE_PASSWORD_KEY + userId);
    }

    /** 更换设备：下发验证凭证，返回 421（前端据此展示验证码验证界面） */
    private BizException newDeviceVerifyException(UcUser user, String deviceId, String deviceType, String ip,
                                                  String userAgent, HttpServletRequest request) {
        String verifyToken = PkuseFoxUtil.getRandomString(32);
        // 以 JSON 字符串存入 Redis（框架 JSON 模板对反序列化类型有白名单限制，此处自行序列化）
        DeviceVerifyBO info = new DeviceVerifyBO(user.getId(), user.getUsername(), deviceId, ip, userAgent);
        info.setDeviceType(deviceType);
        PkuseManager.getSaTokenDao().set(VERIFY_TOKEN_KEY + verifyToken, toJson(info),
                properties.getDeviceVerifyTokenMinutes() * 60L);

        String maskedPhone = user.getPhone() == null ? "" : maskPhone(user.getPhone());
        loginLogService.record(user.getUsername(), user.getId(), "login", UcLoginLog.RESULT_DEVICE_LOCK,
                "更换设备登录：需要验证（原设备 " + userDeviceService.countByUserId(user.getId()) + " 台）",
                deviceId, request);

        // 异常携带的响应数据由全局异常处理器直接返回前端，故使用 VO 承载
        DeviceNeedVerifyVO data = new DeviceNeedVerifyVO();
        data.setNeedVerify(true);
        data.setVerifyToken(verifyToken);
        data.setPhone(maskedPhone);
        return new BizException(CODE_DEVICE_VERIFY, "检测到新设备登录，需验证身份后才能登录", data);
    }

    private DeviceVerifyBO getVerifyInfoOrThrow(String verifyToken) {
        if (verifyToken == null || verifyToken.isBlank()) {
            throw new BizException("验证凭证不能为空");
        }
        String json = PkuseManager.getSaTokenDao().get(VERIFY_TOKEN_KEY + verifyToken);
        if (json == null || json.isBlank()) {
            throw new BizException("验证已过期，请重新登录");
        }
        try {
            return JSON_MAPPER.readValue(json, DeviceVerifyBO.class);
        } catch (Exception e) {
            throw new BizException("验证信息解析失败，请重新登录");
        }
    }

    /** 序列化辅助（验证凭证存储使用） */
    private static String toJson(DeviceVerifyBO info) {
        try {
            return JSON_MAPPER.writeValueAsString(info);
        } catch (Exception e) {
            throw new BizException("验证信息序列化失败：" + e.getMessage());
        }
    }

    /** 解密前端提交的 SM2 密码密文（国密） */
    private String decryptPassword(String cipher) {
        try {
            return sm2KeyManager.decrypt(cipher);
        } catch (Exception e) {
            log.warn("SM2 密码解密失败：{}", e.getMessage());
            throw new BizException("密码解密失败，请刷新页面后重试");
        }
    }

    /** 设备指纹：优先使用前端上报值，为空时按 User-Agent 生成 */
    private static String resolveDeviceId(String deviceId, HttpServletRequest request) {
        if (deviceId != null && !deviceId.isBlank()) {
            return deviceId.length() > 128 ? deviceId.substring(0, 128) : deviceId;
        }
        String userAgent = RequestInfoUtil.getUserAgent(request);
        return "ua-" + PasswordUtil.md5(userAgent == null ? "unknown" : userAgent).substring(0, 24);
    }

    /** 是否豁免设备锁（内置管理员，可配置） */
    private boolean isExemptDeviceLock(UcUser user) {
        return properties.isDeviceLockExemptAdmin() && UcUserService.BUILT_IN_ADMIN_ID.equals(user.getId());
    }

    /** 手机号脱敏：138****0001 */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static String abbreviate(String deviceId) {
        if (deviceId == null) {
            return "";
        }
        return deviceId.length() > 12 ? deviceId.substring(0, 12) + "…" : deviceId;
    }

}
