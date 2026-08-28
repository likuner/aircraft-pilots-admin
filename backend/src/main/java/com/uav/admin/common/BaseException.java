package com.uav.admin.common;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BaseException(String message) {
        super(message);
        this.code = 500;
    }

    public BaseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
