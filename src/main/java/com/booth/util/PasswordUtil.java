package com.booth.util;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 密码工具：MD5 + 固定盐（课程项目演示用，生产环境请使用 BCrypt）
 */
public class PasswordUtil {

    private static final String SALT = "booth#2026";

    public static String encode(String rawPassword) {
        return DigestUtils.md5DigestAsHex((SALT + rawPassword).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean matches(String rawPassword, String encoded) {
        return encode(rawPassword).equals(encoded);
    }
}
