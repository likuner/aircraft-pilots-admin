package com.uav.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    /** 验证码 key（提交登录时回传） */
    private String captchaKey;
    /** base64 图片（不含 data:image 前缀） */
    private String img;
}
