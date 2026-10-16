package com.bdsoft.usercenter.common.config;

import java.util.List;
import java.util.Map;

/**
 * 规则配置项定义（唯一事实来源）
 *
 * <p> 每项包含：配置键、默认值、界面名称、分组、值类型、说明； </p>
 * <p> 后端启动时会按此定义把缺失的配置项写入 uc_config（后续版本新增配置项无需再执行 SQL）， </p>
 * <p> 界面的「恢复默认」也以这里的默认值为准。 </p>
 */
public final class SystemConfigItems {

    private SystemConfigItems() {
    }

    /** 配置分组 */
    public static final String GROUP_PASSWORD = "密码规则";
    public static final String GROUP_LOGIN = "登录规则";
    public static final String GROUP_LOG = "日志规则";

    /** 值类型 */
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INT = "int";
    public static final String TYPE_BOOL = "bool";
    public static final String TYPE_OPTION = "option";

    // ------------------------- 密码规则
    public static final String PASSWORD_COMPLEXITY_ENABLED = "password.complexity-enabled";
    public static final String PASSWORD_MIN_LENGTH = "password.min-length";
    public static final String PASSWORD_REQUIRE_UPPERCASE = "password.require-uppercase";
    public static final String PASSWORD_REQUIRE_LOWERCASE = "password.require-lowercase";
    public static final String PASSWORD_REQUIRE_DIGIT = "password.require-digit";
    public static final String PASSWORD_REQUIRE_SPECIAL = "password.require-special";
    public static final String PASSWORD_DEFAULT = "password.default";

    // ------------------------- 登录规则
    public static final String LOGIN_FORCE_CHANGE_PASSWORD = "login.force-change-password-on-first";
    public static final String LOGIN_DEVICE_VERIFY_ENABLED = "login.device-verify-enabled";
    public static final String LOGIN_KICK_SAME_DEVICE_TYPE = "login.kick-same-device-type";
    public static final String LOGIN_CAPTCHA_ENABLED = "login.captcha-enabled";
    public static final String LOGIN_CAPTCHA_FAIL_THRESHOLD = "login.captcha-fail-threshold";
    public static final String LOGIN_MAX_FAIL_COUNT = "login.max-fail-count";
    public static final String LOGIN_FAIL_WINDOW_SECONDS = "login.fail-window-seconds";
    public static final String LOGIN_LOCK_MINUTES = "login.lock-minutes";

    // ------------------------- 日志规则
    public static final String LOG_LOGIN_PARTITION = "log.login.partition";
    public static final String LOG_OPER_PARTITION = "log.oper.partition";

    /** 分区频率选项 */
    public static final List<Option> PARTITION_OPTIONS = List.of(
            new Option("monthly", "按月分区（每月一个分区）"),
            new Option("weekly", "按周分区（每周一个分区）"),
            new Option("daily", "按天分区（每天一个分区）"));

    /** int 类型配置项的取值范围（key → [min, max]） */
    public static final Map<String, int[]> INT_RANGES = Map.of(
            PASSWORD_MIN_LENGTH, new int[]{6, 32},
            LOGIN_CAPTCHA_FAIL_THRESHOLD, new int[]{0, 20},
            LOGIN_MAX_FAIL_COUNT, new int[]{1, 20},
            LOGIN_FAIL_WINDOW_SECONDS, new int[]{60, 86400},
            LOGIN_LOCK_MINUTES, new int[]{1, 1440});

    /** 全部配置项（顺序即界面展示顺序） */
    public static final List<Item> ALL = List.of(
            new Item(PASSWORD_COMPLEXITY_ENABLED, "false", "启用密码复杂度校验", GROUP_PASSWORD, TYPE_BOOL,
                    "关闭时仅校验最小长度 6 位；开启后按下列规则校验（新增用户、重置密码、修改密码均生效）"),
            new Item(PASSWORD_MIN_LENGTH, "8", "密码最小长度", GROUP_PASSWORD, TYPE_INT, "取值范围 6 ~ 32"),
            new Item(PASSWORD_REQUIRE_UPPERCASE, "true", "必须包含大写字母", GROUP_PASSWORD, TYPE_BOOL, "A-Z"),
            new Item(PASSWORD_REQUIRE_LOWERCASE, "true", "必须包含小写字母", GROUP_PASSWORD, TYPE_BOOL, "a-z"),
            new Item(PASSWORD_REQUIRE_DIGIT, "true", "必须包含数字", GROUP_PASSWORD, TYPE_BOOL, "0-9"),
            new Item(PASSWORD_REQUIRE_SPECIAL, "false", "必须包含特殊字符", GROUP_PASSWORD, TYPE_BOOL, "字母与数字之外的字符，如 !@#$%"),
            new Item(PASSWORD_DEFAULT, "123456", "系统默认密码", GROUP_PASSWORD, TYPE_STRING,
                    "新增用户未填写密码、以及管理员重置密码时的默认值（需满足密码复杂度要求）"),

            new Item(LOGIN_FORCE_CHANGE_PASSWORD, "false", "首次登录强制修改密码", GROUP_LOGIN, TYPE_BOOL,
                    "开启后：新增用户、被重置密码的用户下次登录需先修改密码才能使用系统"),
            new Item(LOGIN_DEVICE_VERIFY_ENABLED, "true", "启用设备验证（设备锁）", GROUP_LOGIN, TYPE_BOOL,
                    "开启后：账号首次登录自动绑定设备指纹，更换设备登录需短信验证码验证后换绑"),
            new Item(LOGIN_KICK_SAME_DEVICE_TYPE, "false", "踢掉同类型设备在线账号", GROUP_LOGIN, TYPE_BOOL,
                    "开启后：同一账号在 PC / 移动端 / 小程序内可分别同时在线，但同一设备类型内新登录会踢掉该类型已在线的会话；关闭则各类型均可多端并存"),
            new Item(LOGIN_CAPTCHA_ENABLED, "true", "启用滑块验证码", GROUP_LOGIN, TYPE_BOOL,
                    "关闭后登录不再需要滑块验证码"),
            new Item(LOGIN_CAPTCHA_FAIL_THRESHOLD, "0", "验证码触发阈值（0=每次登录都需验证）", GROUP_LOGIN, TYPE_INT,
                    "0=每次登录都需要；N=连续密码错误 N 次后需要，取值范围 0 ~ 20"),
            new Item(LOGIN_MAX_FAIL_COUNT, "5", "连续密码错误锁定阈值", GROUP_LOGIN, TYPE_INT, "达到该次数后锁定账号，取值范围 1 ~ 20"),
            new Item(LOGIN_FAIL_WINDOW_SECONDS, "600", "失败计数窗口（秒）", GROUP_LOGIN, TYPE_INT, "窗口期内的连续失败参与计数，取值范围 60 ~ 86400"),
            new Item(LOGIN_LOCK_MINUTES, "30", "锁定时长（分钟）", GROUP_LOGIN, TYPE_INT, "锁定期内禁止登录，可由管理员在线解锁，取值范围 1 ~ 1440"),

            new Item(LOG_LOGIN_PARTITION, "monthly", "登录日志分区频率", GROUP_LOG, TYPE_OPTION,
                    "修改后立即生效：后端按新频率续建分区（已有分区保持不变，区间连续）", PARTITION_OPTIONS),
            new Item(LOG_OPER_PARTITION, "monthly", "操作日志分区频率", GROUP_LOG, TYPE_OPTION,
                    "修改后立即生效：后端按新频率续建分区（已有分区保持不变，区间连续）", PARTITION_OPTIONS));

    /** 按配置键查找定义（不存在返回 null） */
    public static Item find(String key) {
        if (key == null) {
            return null;
        }
        for (Item item : ALL) {
            if (item.key().equals(key)) {
                return item;
            }
        }
        return null;
    }

    /** 配置项定义 */
    public record Item(String key, String defaultValue, String name, String group, String type, String remark,
                       List<Option> options) {

        public Item(String key, String defaultValue, String name, String group, String type, String remark) {
            this(key, defaultValue, name, group, type, remark, List.of());
        }
    }

    /** 选项（option 类型配置项的可选值） */
    public record Option(String value, String label) {
    }

}
