package com.ddingjoo.urlshortener.exception.types;

import com.ddingjoo.urlshortener.exception.core.ErrorCode;
import com.ddingjoo.urlshortener.exception.core.UrlShortenerException;

public final class RateLimitExceededException extends UrlShortenerException {
	
	public RateLimitExceededException() {
		super(ErrorCode.RATE_LIMIT_EXCEEDED);
	}
}
