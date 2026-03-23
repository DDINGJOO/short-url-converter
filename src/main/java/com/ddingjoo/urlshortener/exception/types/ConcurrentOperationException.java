package com.ddingjoo.urlshortener.exception.types;

import com.ddingjoo.urlshortener.exception.core.ErrorCode;
import com.ddingjoo.urlshortener.exception.core.UrlShortenerException;

public final class ConcurrentOperationException extends UrlShortenerException {
	
	public ConcurrentOperationException() {
		super(ErrorCode.CONCURRENT_OPERATION);
	}
}
