package com.booth.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码工具（4 位字母数字），内存存储，5 分钟过期
 */
public class CaptchaUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final ConcurrentHashMap<String, CaptchaEntry> STORE = new ConcurrentHashMap<>();
    private static final long TTL = 5 * 60 * 1000L;

    private static class CaptchaEntry {
        String text;
        long expireAt;

        CaptchaEntry(String text, long expireAt) {
            this.text = text;
            this.expireAt = expireAt;
        }
    }

    /** 生成验证码图片，返回 base64 字符串（不含 data:image 前缀） */
    public static String generate(String captchaId) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        String text = sb.toString();
        STORE.put(captchaId, new CaptchaEntry(text, System.currentTimeMillis() + TTL));

        int width = 120, height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, width, height);
        // 干扰线
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200)));
            g.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height), RANDOM.nextInt(width), RANDOM.nextInt(height));
        }
        g.setFont(new Font("Arial", Font.BOLD, 26));
        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(30 + RANDOM.nextInt(120), 60 + RANDOM.nextInt(120), 140 + RANDOM.nextInt(80)));
            g.drawString(String.valueOf(text.charAt(i)), 14 + i * 26, 30);
        }
        g.dispose();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("验证码生成失败", e);
        }
    }

    /** 校验验证码（忽略大小写），校验后即失效 */
    public static boolean verify(String captchaId, String code) {
        if (captchaId == null || code == null) {
            return false;
        }
        CaptchaEntry entry = STORE.remove(captchaId);
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            return false;
        }
        return entry.text.equalsIgnoreCase(code.trim());
    }
}
