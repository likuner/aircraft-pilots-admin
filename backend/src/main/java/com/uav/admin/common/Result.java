package com.uav.admin.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码：200 成功，其它失败 */
    private int code;
    /** 提示信息 */
    private String msg;
    /** 数据 */
    private T data;

    public static <T> Result<T> ok() {
        return build(200, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return build(200, "操作成功", data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return build(200, msg, data);
    }

    public static <T> Result<T> fail(String msg) {
        return build(500, msg, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return build(code, msg, null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return build(errorCode.getCode(), errorCode.getMsg(), null);
    }

    public static <T> Result<T> build(int code, String msg, T data) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setData(data);
        return r;
    }
}
