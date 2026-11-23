package com.bdsoft.usercenter.common.util;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * 密码摘要工具（国密 SM3 + 随机盐）
 *
 * <pre>
 *   存储格式：password = SM3(salt + ":" + 明文)；salt 为随机十六进制串（用户表 password_salt 列）。
 *   兼容历史数据：未设置 salt 的账号沿用 MD5 校验，校验通过后自动升级为 SM3 存储（无感升级）。
 * </pre>
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 盐长度（字节） */
    private static final int SALT_BYTES = 16;

    private PasswordUtil() {
    }

    /** 生成随机盐（32 位十六进制） */
    public static String newSalt() {
        byte[] bytes = new byte[SALT_BYTES];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    /** 国密 SM3 摘要：SM3(salt + ":" + 明文) */
    public static String encode(String rawPassword, String salt) {
        return SmCryptoUtil.sm3(salt + ":" + rawPassword);
    }

    /**
     * 校验密码
     *
     * @param rawPassword 明文
     * @param storedHash  数据库中的密码摘要
     * @param salt        数据库中的盐（为空表示历史 MD5 数据）
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String storedHash, String salt) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        if (salt == null || salt.isBlank()) {
            // 历史数据：MD5(明文)
            return md5(rawPassword).equalsIgnoreCase(storedHash);
        }
        return encode(rawPassword, salt).equalsIgnoreCase(storedHash);
    }

    /** 是否为历史 MD5 摘要（需要升级为 SM3） */
    public static boolean needUpgrade(String storedHash, String salt) {
        return storedHash != null && storedHash.length() == 32 && (salt == null || salt.isBlank());
    }

    /** MD5（仅用于兼容历史数据与设备指纹派生） */
    public static String md5(String text) {
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }

}
