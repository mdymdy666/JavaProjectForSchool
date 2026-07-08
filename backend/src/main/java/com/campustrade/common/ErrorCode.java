package com.campustrade.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "请先登录", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "无权执行该操作", HttpStatus.FORBIDDEN),
    PRODUCT_NOT_FOUND(40401, "商品不存在", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(40402, "订单不存在", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(40403, "用户不存在", HttpStatus.NOT_FOUND),
    INVALID_STATE(40901, "当前状态不允许该操作", HttpStatus.CONFLICT),
    DUPLICATE_RESOURCE(40902, "数据已存在", HttpStatus.CONFLICT),
    RATE_LIMITED(42900, "操作过于频繁，请稍后再试", HttpStatus.TOO_MANY_REQUESTS),
    CAPTCHA_INVALID(40001, "验证码错误或已过期", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
