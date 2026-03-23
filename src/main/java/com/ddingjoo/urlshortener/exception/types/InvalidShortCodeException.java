package com.ddingjoo.urlshortener.exception.types;

import com.ddingjoo.urlshortener.exception.core.ErrorCode;
import com.ddingjoo.urlshortener.exception.core.UrlShortenerException;

public final class InvalidShortCodeException extends UrlShortenerException {
	
	public InvalidShortCodeException(String message) {
		super(ErrorCode.INVALID_SHORT_CODE, message);
	}
}
