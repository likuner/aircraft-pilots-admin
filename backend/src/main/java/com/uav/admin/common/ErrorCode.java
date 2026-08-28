package com.uav.admin.common;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无操作权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // 认证相关
    CAPTCHA_ERROR(1001, "验证码错误或已过期"),
    LOGIN_FAIL(1002, "用户名或密码错误"),
    ACCOUNT_DISABLED(1003, "账号已被禁用"),
    TOKEN_INVALID(1004, "Token 无效"),
    PASSWORD_ERROR(1005, "原密码错误"),
    USER_NOT_FOUND(1006, "用户不存在"),
    PARAM_ERROR(1007, "参数错误"),

    // 业务相关
    STATE_ERROR(2001, "当前状态不允许该操作"),
    DATA_EXISTS(2002, "数据已存在"),
    DATA_NOT_FOUND(2003, "数据不存在"),
    CAPACITY_FULL(2004, "名额已满"),
    ALREADY_SIGNED(2005, "已报名该场次"),
    SCORE_REQUIRED(2006, "请先录入分数"),
    NOT_PASS(2007, "成绩不合格，无法申请证书"),
    CERT_EXISTS(2008, "该报名已存在合格证申请"),
    MATERIAL_REQUIRED(2009, "请先提交申请材料");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
