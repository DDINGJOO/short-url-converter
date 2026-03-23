package com.ddingjoo.urlshortener.exception.types;

import com.ddingjoo.urlshortener.exception.core.ErrorCode;
import com.ddingjoo.urlshortener.exception.core.UrlShortenerException;

public final class UnauthorizedException extends UrlShortenerException {
	
	public UnauthorizedException() {
		super(ErrorCode.UNAUTHORIZED_API_KEY);
	}
}
