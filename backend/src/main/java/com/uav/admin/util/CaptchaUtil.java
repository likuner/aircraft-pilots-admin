package com.uav.admin.util;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;

/**
 * 图形验证码工具（数学算式）
 */
public class CaptchaUtil {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final Random RANDOM = new Random();

    /**
     * 生成验证码，返回 {text, base64}
     */
    public static String[] generate() {
        int a = RANDOM.nextInt(20) + 1;
        int b = RANDOM.nextInt(20) + 1;
        String text = a + "+" + b + "=?";
        String answer = String.valueOf(a + b);

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线
        g.setColor(new Color(200, 210, 220));
        g.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 5; i++) {
            g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                    RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
        }

        // 噪点
        g.setColor(new Color(180, 190, 200));
        for (int i = 0; i < 40; i++) {
            g.fillRect(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 2, 2);
        }

        // 算式文本
        g.setColor(new Color(40, 80, 160));
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString(a + "+" + b + "=?", 8, 28);

        g.dispose();

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bos);
            String base64 = Base64.getEncoder().encodeToString(bos.toByteArray());
            return new String[]{answer, base64};
        } catch (Exception e) {
            throw new RuntimeException("验证码生成失败", e);
        }
    }
}
