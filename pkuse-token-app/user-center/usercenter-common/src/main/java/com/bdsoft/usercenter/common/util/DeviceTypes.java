package com.bdsoft.usercenter.common.util;

/**
 * 设备类型定义与解析
 *
 * <p> 同一账号可在 PC、移动端、小程序三类设备上同时在线； </p>
 * <p> 「是否踢掉同类型设备在线账号」（规则配置 login.kick-same-device-type）开启时，
 *     新登录会踢掉<b>同设备类型</b>的已在线会话，其它类型不受影响。 </p>
 */
public final class DeviceTypes {

    private DeviceTypes() {
    }

    /** PC 端（浏览器 / 桌面应用） */
    public static final String PC = "pc";

    /** 移动端（手机 / 平板 App、移动端 H5） */
    public static final String MOBILE = "mobile";

    /** 小程序（微信 / 支付宝等小程序） */
    public static final String MINI = "mini";

    /** 移动端 User-Agent 特征关键词 */
    private static final String[] MOBILE_UA_KEYWORDS = {
            "mobile", "android", "iphone", "ipad", "ipod", "harmonyos", "micromessenger"
    };

    /**
     * 解析设备类型：优先使用调用方显式上报值；未上报时按 User-Agent 推断（移动端特征 → mobile，否则 → pc）
     *
     * @param deviceType 上报的设备类型（可为空）
     * @param userAgent 请求 User-Agent（可为空）
     * @return 归一化后的设备类型（pc / mobile / mini）
     */
    public static String resolve(String deviceType, String userAgent) {
        if (deviceType != null && !deviceType.isBlank()) {
            return normalize(deviceType);
        }
        String ua = userAgent == null ? "" : userAgent.toLowerCase();
        for (String keyword : MOBILE_UA_KEYWORDS) {
            if (ua.contains(keyword)) {
                return MOBILE;
            }
        }
        return PC;
    }

    /** 归一化设备类型：转小写去空白；标准值之外的显式类型原样保留（自成一类参与互踢） */
    public static String normalize(String deviceType) {
        return deviceType == null ? "" : deviceType.trim().toLowerCase();
    }

}
