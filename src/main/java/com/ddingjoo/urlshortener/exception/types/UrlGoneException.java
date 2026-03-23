package com.ddingjoo.urlshortener.exception.types;

import com.ddingjoo.urlshortener.exception.core.ErrorCode;
import com.ddingjoo.urlshortener.exception.core.UrlShortenerException;

public final class UrlGoneException extends UrlShortenerException {
	
	public UrlGoneException() {
		super(ErrorCode.URL_GONE);
	}
}
