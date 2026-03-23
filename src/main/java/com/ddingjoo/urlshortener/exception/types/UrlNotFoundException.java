package com.ddingjoo.urlshortener.exception.types;

import com.ddingjoo.urlshortener.exception.core.ErrorCode;
import com.ddingjoo.urlshortener.exception.core.UrlShortenerException;

public final class UrlNotFoundException extends UrlShortenerException {
	
	public UrlNotFoundException() {
		super(ErrorCode.URL_NOT_FOUND);
	}
}
