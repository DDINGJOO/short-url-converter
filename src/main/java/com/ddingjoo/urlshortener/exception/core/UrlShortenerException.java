package com.ddingjoo.urlshortener.exception.core;

import com.ddingjoo.urlshortener.exception.types.*;

public sealed abstract class UrlShortenerException extends RuntimeException
		permits CodeConflictException, InvalidShortCodeException, InvalidUrlException,
		RateLimitExceededException, UnauthorizedException, UrlGoneException, UrlNotFoundException,
		ConcurrentOperationException {
	
	private final ErrorCode errorCode;
	
	protected UrlShortenerException(ErrorCode errorCode) {
		super(errorCode.defaultMessage());
		this.errorCode = errorCode;
	}
	
	protected UrlShortenerException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
	
	public ErrorCode errorCode() {
		return errorCode;
	}
}
