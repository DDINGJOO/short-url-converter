package com.ddingjoo.urlshortener.exception.types;

import com.ddingjoo.urlshortener.exception.core.ErrorCode;
import com.ddingjoo.urlshortener.exception.core.UrlShortenerException;

public final class InvalidUrlException extends UrlShortenerException {
	
	public InvalidUrlException(String message) {
		super(ErrorCode.INVALID_URL, message);
	}
}
