package com.ddingjoo.urlshortener.exception.core;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	SHORT_CODE_CONFLICT(HttpStatus.CONFLICT, "SHORT_CODE_CONFLICT", "Custom short code already exists"),
	SHORT_CODE_GENERATION_EXHAUSTED(HttpStatus.CONFLICT, "SHORT_CODE_GENERATION_EXHAUSTED", "Failed to generate a unique short code after retry budget"),
	INVALID_SHORT_CODE(HttpStatus.BAD_REQUEST, "INVALID_SHORT_CODE", "custom_code is invalid"),
	INVALID_SHORT_CODE_PATTERN(HttpStatus.BAD_REQUEST, "INVALID_SHORT_CODE_PATTERN", "custom_code must match [A-Za-z0-9_-]{1,20}"),
	INVALID_URL(HttpStatus.BAD_REQUEST, "INVALID_URL", "Request URL is invalid"),
	INVALID_URL_SCHEME(HttpStatus.BAD_REQUEST, "INVALID_URL_SCHEME", "original_url must use http or https"),
	INVALID_URL_HOST(HttpStatus.BAD_REQUEST, "INVALID_URL_HOST", "original_url must include a valid host"),
	INVALID_URL_SYNTAX(HttpStatus.BAD_REQUEST, "INVALID_URL_SYNTAX", "original_url is not a valid URI"),
	INVALID_EXPIRATION(HttpStatus.BAD_REQUEST, "INVALID_EXPIRATION", "expires_at must be in the future"),
	INVALID_ANALYTICS_GRANULARITY(HttpStatus.BAD_REQUEST, "INVALID_ANALYTICS_GRANULARITY", "granularity must be one of [hour, day]"),
	INVALID_ANALYTICS_RANGE(HttpStatus.BAD_REQUEST, "INVALID_ANALYTICS_RANGE", "from must be before or equal to to"),
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
