package com.ddingjoo.urlshortener.exception.types;


import com.ddingjoo.urlshortener.exception.core.ErrorCode;
import com.ddingjoo.urlshortener.exception.core.UrlShortenerException;

public final class CodeConflictException extends UrlShortenerException {
	
	public CodeConflictException() {
		super(ErrorCode.SHORT_CODE_CONFLICT);
	}
	
	public CodeConflictException(String message) {
		super(ErrorCode.SHORT_CODE_CONFLICT, message);
	}
}
