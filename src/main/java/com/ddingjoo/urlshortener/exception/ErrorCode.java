package com.ddingjoo.urlshortener.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SHORT_CODE_CONFLICT(HttpStatus.CONFLICT, "SHORT_CODE_CONFLICT", "Custom short code already exists"),
    INVALID_SHORT_CODE(HttpStatus.BAD_REQUEST, "INVALID_SHORT_CODE", "custom_code is invalid"),
    INVALID_URL(HttpStatus.BAD_REQUEST, "INVALID_URL", "Request URL is invalid"),
    URL_NOT_FOUND(HttpStatus.NOT_FOUND, "URL_NOT_FOUND", "Short URL not found"),
    URL_GONE(HttpStatus.GONE, "URL_GONE", "Short URL is no longer active"),
    CONCURRENT_OPERATION(HttpStatus.CONFLICT, "CONCURRENT_OPERATION", "Concurrent operation is in progress"),
    UNAUTHORIZED_API_KEY(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_API_KEY", "Invalid admin API key"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Rate limit exceeded");

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
