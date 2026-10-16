package com.bdsoft.usercenter.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 请求信息工具：客户端 IP / User-Agent 解析
 */
public final class RequestInfoUtil {

    private RequestInfoUtil() {
    }

    /** 获取客户端 IP（兼容反向代理场景） */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For 可能是逗号分隔的链路，取第一个
                int index = value.indexOf(',');
                return (index > 0 ? value.substring(0, index) : value).trim();
            }
        }
        return request.getRemoteAddr();
    }

    /** 获取 User-Agent（超长截断，避免超出列宽） */
    public static String getUserAgent(HttpServletRequest request) {
        String ua = request == null ? null : request.getHeader("User-Agent");
        if (ua == null) {
            return null;
        }
        return ua.length() > 500 ? ua.substring(0, 500) : ua;
    }

    /** 从 User-Agent 粗略解析操作系统 */
    public static String parseOs(String userAgent) {
        if (userAgent == null) {
            return "未知";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows nt 10")) {
            return "Windows 10/11";
        }
        if (ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return "iOS";
        }
        if (ua.contains("mac os x")) {
            return "macOS";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return "未知";
    }

    /** 从 User-Agent 粗略解析浏览器 */
    public static String parseBrowser(String userAgent) {
        if (userAgent == null) {
            return "未知";
        }
        String ua = userAgent.toLowerCase();
        // 顺序敏感：Chrome 的 UA 里也包含 Safari/AppleWebKit
        if (ua.contains("edg/")) {
            return "Edge";
        }
        if (ua.contains("opr/") || ua.contains("opera")) {
            return "Opera";
        }
        if (ua.contains("firefox")) {
            return "Firefox";
        }
        if (ua.contains("chrome")) {
            return "Chrome";
        }
        if (ua.contains("safari")) {
            return "Safari";
        }
        if (ua.contains("curl") || ua.contains("java")) {
            return "其他客户端";
        }
        return "未知";
    }

}
