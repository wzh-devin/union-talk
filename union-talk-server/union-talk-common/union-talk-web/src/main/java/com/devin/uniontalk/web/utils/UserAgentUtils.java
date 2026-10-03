package com.devin.uniontalk.web.utils;

import com.devin.uniontalk.infrastructure.user.enums.DeviceTypeEnum;

/**
 * 2026/5/16 23:20.
 *
 * <p>
 * User-Agent 解析工具类
 * </p>
 *
 * @author <a href="https://github.com/wzh-devin">devin</a>
 * @version 1.0.0
 * @since 1.0.0
 */
public final class UserAgentUtils {

    private UserAgentUtils() {
    }

    /**
     * 从 User-Agent 解析设备类型.
     *
     * @param userAgent User-Agent 字符串
     * @return 设备类型枚举
     */
    public static DeviceTypeEnum parseDeviceType(final String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return DeviceTypeEnum.UNKNOWN;
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("android")) {
            return DeviceTypeEnum.ANDROID;
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) {
            return DeviceTypeEnum.IOS;
        }
        if (ua.contains("electron") || ua.contains("desktop")) {
            return DeviceTypeEnum.DESKTOP;
        }
        if (ua.contains("mozilla") || ua.contains("chrome") || ua.contains("safari") || ua.contains("firefox")) {
            return DeviceTypeEnum.WEB;
        }
        return DeviceTypeEnum.UNKNOWN;
    }

    /**
     * 从 User-Agent 解析设备名称（浏览器或客户端名称）.
     *
     * @param userAgent User-Agent 字符串
     * @return 设备名称
     */
    public static String parseDeviceName(final String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }
        // 按优先级匹配浏览器标识
        if (userAgent.contains("Edg/")) {
            return extractVersion("Edge", userAgent, "Edg/");
        }
        if (userAgent.contains("OPR/") || userAgent.contains("Opera")) {
            return extractVersion("Opera", userAgent, "OPR/");
        }
        if (userAgent.contains("Chrome/") && !userAgent.contains("Edg/")) {
            return extractVersion("Chrome", userAgent, "Chrome/");
        }
        if (userAgent.contains("Firefox/")) {
            return extractVersion("Firefox", userAgent, "Firefox/");
        }
        if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) {
            return extractVersion("Safari", userAgent, "Version/");
        }
        return "Unknown";
    }

    /**
     * 提取浏览器名称和主版本号.
     *
     * @param name  浏览器名称
     * @param ua    User-Agent
     * @param token 浏览器标识
     * @return 浏览器名称和主版本号
     */
    private static String extractVersion(final String name, final String ua, final String token) {
        int idx = ua.indexOf(token);
        if (idx < 0) {
            return name;
        }
        int start = idx + token.length();
        int end = start;
        while (end < ua.length() && ua.charAt(end) != ' ' && ua.charAt(end) != ';') {
            end++;
        }
        String version = ua.substring(start, end);
        // 只取主版本号
        int dotIdx = version.indexOf('.');
        if (dotIdx > 0) {
            version = version.substring(0, dotIdx);
        }
        return name + " " + version;
    }
}
