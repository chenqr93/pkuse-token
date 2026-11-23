package com.bdsoft.usercenter.auth.service.impl;

import com.bdsoft.pkusetoken.PkuseManager;
import com.bdsoft.usercenter.common.config.SystemConfig;
import com.bdsoft.usercenter.auth.service.LoginAttemptService;
import com.bdsoft.usercenter.config.service.UcConfigService;
import org.springframework.stereotype.Service;

/**
 * 登录失败计数 Service 实现（基于框架 PkuseTokenDao，即 Redis）
 *
 * <p> 计数窗口与验证码策略来自「规则配置」（uc_config），修改后即时生效。 </p>
 */
@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    /** 计数 key 前缀 */
    private static final String KEY_PREFIX = "uc:login:fail:";

    private final UcConfigService configService;

    public LoginAttemptServiceImpl(UcConfigService configService) {
        this.configService = configService;
    }

    @Override
    public long getFailCount(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        String value = PkuseManager.getSaTokenDao().get(key(username));
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public long increaseFailCount(String username) {
        long count = getFailCount(username) + 1;
        // 窗口期内累计，窗口过期后自动清零（窗口时长见规则配置 login.fail-window-seconds）
        PkuseManager.getSaTokenDao().set(key(username), String.valueOf(count),
                configService.get().getFailWindowSeconds());
        return count;
    }

    @Override
    public void clearFailCount(String username) {
        PkuseManager.getSaTokenDao().delete(key(username));
    }

    @Override
    public boolean needCaptcha(String username) {
        SystemConfig config = configService.get();
        if (!config.isCaptchaEnabled()) {
            // 规则配置：关闭滑块验证码
            return false;
        }
        int threshold = config.getCaptchaFailThreshold();
        if (threshold <= 0) {
            // 0：每次登录都需要
            return true;
        }
        return getFailCount(username) >= threshold;
    }

    private static String key(String username) {
        return KEY_PREFIX + username;
    }

}
