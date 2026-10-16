package com.bdsoft.usercenter.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 规则配置运行时快照（由 uc_config 表按 {@link SystemConfigItems} 的定义解析而来）
 *
 * <p> 配置变更后由 UcConfigService 重新构建并整体替换（volatile 引用），读侧无锁； </p>
 * <p> 缺失的配置项自动回落到 {@link SystemConfigItems} 中定义的默认值。 </p>
 */
public class SystemConfig {

    /** 密码复杂度关闭时的最小长度（历史行为） */
    private static final int LEGACY_MIN_LENGTH = 6;

    private boolean passwordComplexityEnabled;
    private int passwordMinLength;
    private boolean passwordRequireUppercase;
    private boolean passwordRequireLowercase;
    private boolean passwordRequireDigit;
    private boolean passwordRequireSpecial;
    private String defaultPassword;

    private boolean forceChangePasswordOnFirst;
    private boolean deviceVerifyEnabled;
    private boolean kickSameDeviceType;
    private boolean captchaEnabled;
    private int captchaFailThreshold;
    private int maxFailCount;
    private int failWindowSeconds;
    private int lockMinutes;

    private String loginLogPartition;
    private String operLogPartition;

    /** 全部取默认值 */
    public static SystemConfig defaults() {
        return parse(Map.of());
    }

    /** 由「配置键 → 配置值」解析（缺失项回落到默认值） */
    public static SystemConfig parse(Map<String, String> values) {
        SystemConfig config = new SystemConfig();
        for (SystemConfigItems.Item item : SystemConfigItems.ALL) {
            String raw = values.get(item.key());
            config.apply(item.key(), raw == null || raw.isBlank() ? item.defaultValue() : raw.trim());
        }
        return config;
    }

    /** 配置键 → 配置值（落库用） */
    public static Map<String, String> toMap(SystemConfig config) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(SystemConfigItems.PASSWORD_COMPLEXITY_ENABLED, String.valueOf(config.passwordComplexityEnabled));
        map.put(SystemConfigItems.PASSWORD_MIN_LENGTH, String.valueOf(config.passwordMinLength));
        map.put(SystemConfigItems.PASSWORD_REQUIRE_UPPERCASE, String.valueOf(config.passwordRequireUppercase));
        map.put(SystemConfigItems.PASSWORD_REQUIRE_LOWERCASE, String.valueOf(config.passwordRequireLowercase));
        map.put(SystemConfigItems.PASSWORD_REQUIRE_DIGIT, String.valueOf(config.passwordRequireDigit));
        map.put(SystemConfigItems.PASSWORD_REQUIRE_SPECIAL, String.valueOf(config.passwordRequireSpecial));
        map.put(SystemConfigItems.PASSWORD_DEFAULT, config.defaultPassword);
        map.put(SystemConfigItems.LOGIN_FORCE_CHANGE_PASSWORD, String.valueOf(config.forceChangePasswordOnFirst));
        map.put(SystemConfigItems.LOGIN_DEVICE_VERIFY_ENABLED, String.valueOf(config.deviceVerifyEnabled));
        map.put(SystemConfigItems.LOGIN_KICK_SAME_DEVICE_TYPE, String.valueOf(config.kickSameDeviceType));
        map.put(SystemConfigItems.LOGIN_CAPTCHA_ENABLED, String.valueOf(config.captchaEnabled));
        map.put(SystemConfigItems.LOGIN_CAPTCHA_FAIL_THRESHOLD, String.valueOf(config.captchaFailThreshold));
        map.put(SystemConfigItems.LOGIN_MAX_FAIL_COUNT, String.valueOf(config.maxFailCount));
        map.put(SystemConfigItems.LOGIN_FAIL_WINDOW_SECONDS, String.valueOf(config.failWindowSeconds));
        map.put(SystemConfigItems.LOGIN_LOCK_MINUTES, String.valueOf(config.lockMinutes));
        map.put(SystemConfigItems.LOG_LOGIN_PARTITION, config.loginLogPartition);
        map.put(SystemConfigItems.LOG_OPER_PARTITION, config.operLogPartition);
        return map;
    }

    private void apply(String key, String value) {
        switch (key) {
            case SystemConfigItems.PASSWORD_COMPLEXITY_ENABLED -> passwordComplexityEnabled = parseBool(value, false);
            case SystemConfigItems.PASSWORD_MIN_LENGTH -> passwordMinLength = parseInt(value, 8);
            case SystemConfigItems.PASSWORD_REQUIRE_UPPERCASE -> passwordRequireUppercase = parseBool(value, true);
            case SystemConfigItems.PASSWORD_REQUIRE_LOWERCASE -> passwordRequireLowercase = parseBool(value, true);
            case SystemConfigItems.PASSWORD_REQUIRE_DIGIT -> passwordRequireDigit = parseBool(value, true);
            case SystemConfigItems.PASSWORD_REQUIRE_SPECIAL -> passwordRequireSpecial = parseBool(value, false);
            case SystemConfigItems.PASSWORD_DEFAULT -> defaultPassword = value;
            case SystemConfigItems.LOGIN_FORCE_CHANGE_PASSWORD -> forceChangePasswordOnFirst = parseBool(value, false);
            case SystemConfigItems.LOGIN_DEVICE_VERIFY_ENABLED -> deviceVerifyEnabled = parseBool(value, true);
            case SystemConfigItems.LOGIN_KICK_SAME_DEVICE_TYPE -> kickSameDeviceType = parseBool(value, false);
            case SystemConfigItems.LOGIN_CAPTCHA_ENABLED -> captchaEnabled = parseBool(value, true);
            case SystemConfigItems.LOGIN_CAPTCHA_FAIL_THRESHOLD -> captchaFailThreshold = parseInt(value, 0);
            case SystemConfigItems.LOGIN_MAX_FAIL_COUNT -> maxFailCount = parseInt(value, 5);
            case SystemConfigItems.LOGIN_FAIL_WINDOW_SECONDS -> failWindowSeconds = parseInt(value, 600);
            case SystemConfigItems.LOGIN_LOCK_MINUTES -> lockMinutes = parseInt(value, 30);
            case SystemConfigItems.LOG_LOGIN_PARTITION -> loginLogPartition = value;
            case SystemConfigItems.LOG_OPER_PARTITION -> operLogPartition = value;
            default -> {
                // 未知配置键：忽略（校验阶段已拦截）
            }
        }
    }

    /**
     * 密码复杂度校验
     *
     * @return 通过返回 null；不通过返回错误说明（可直接提示给用户）
     */
    public String checkPassword(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "密码不能为空";
        }
        if (!passwordComplexityEnabled) {
            // 未开启复杂度校验：沿用历史行为（最小长度）
            return raw.length() >= LEGACY_MIN_LENGTH ? null : "密码长度不能小于 " + LEGACY_MIN_LENGTH + " 位";
        }
        if (raw.length() < passwordMinLength) {
            return "密码长度不能小于 " + passwordMinLength + " 位";
        }
        if (passwordRequireUppercase && raw.chars().noneMatch(Character::isUpperCase)) {
            return "密码必须包含大写字母";
        }
        if (passwordRequireLowercase && raw.chars().noneMatch(Character::isLowerCase)) {
            return "密码必须包含小写字母";
        }
        if (passwordRequireDigit && raw.chars().noneMatch(Character::isDigit)) {
            return "密码必须包含数字";
        }
        if (passwordRequireSpecial && raw.chars().allMatch(Character::isLetterOrDigit)) {
            return "密码必须包含特殊字符";
        }
        return null;
    }

    /** 密码规则文案（登录/改密页面提示用），如：至少 8 位，须包含大写字母、小写字母、数字 */
    public String passwordRuleText() {
        if (!passwordComplexityEnabled) {
            return "密码长度不少于 " + LEGACY_MIN_LENGTH + " 位";
        }
        StringBuilder sb = new StringBuilder("密码长度不少于 ").append(passwordMinLength).append(" 位");
        StringBuilder classes = new StringBuilder();
        if (passwordRequireUppercase) {
            classes.append("大写字母、");
        }
        if (passwordRequireLowercase) {
            classes.append("小写字母、");
        }
        if (passwordRequireDigit) {
            classes.append("数字、");
        }
        if (passwordRequireSpecial) {
            classes.append("特殊字符、");
        }
        if (!classes.isEmpty()) {
            sb.append("，须包含").append(classes.substring(0, classes.length() - 1));
        }
        return sb.toString();
    }

    private static boolean parseBool(String value, boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String text = value.trim().toLowerCase();
        if ("true".equals(text) || "1".equals(text) || "yes".equals(text)) {
            return true;
        }
        if ("false".equals(text) || "0".equals(text) || "no".equals(text)) {
            return false;
        }
        return defaultValue;
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ------------------------- getter

    public boolean isPasswordComplexityEnabled() {
        return passwordComplexityEnabled;
    }

    public int getPasswordMinLength() {
        return passwordMinLength;
    }

    public boolean isPasswordRequireUppercase() {
        return passwordRequireUppercase;
    }

    public boolean isPasswordRequireLowercase() {
        return passwordRequireLowercase;
    }

    public boolean isPasswordRequireDigit() {
        return passwordRequireDigit;
    }

    public boolean isPasswordRequireSpecial() {
        return passwordRequireSpecial;
    }

    public String getDefaultPassword() {
        return defaultPassword;
    }

    public boolean isForceChangePasswordOnFirst() {
        return forceChangePasswordOnFirst;
    }

    public boolean isDeviceVerifyEnabled() {
        return deviceVerifyEnabled;
    }

    public boolean isKickSameDeviceType() {
        return kickSameDeviceType;
    }

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }

    public int getCaptchaFailThreshold() {
        return captchaFailThreshold;
    }

    public int getMaxFailCount() {
        return maxFailCount;
    }

    public int getFailWindowSeconds() {
        return failWindowSeconds;
    }

    public int getLockMinutes() {
        return lockMinutes;
    }

    public String getLoginLogPartition() {
        return loginLogPartition;
    }

    public String getOperLogPartition() {
        return operLogPartition;
    }

}
