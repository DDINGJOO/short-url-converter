package com.ddingjoo.urlshortener.exception;

public final class CodeConflictException extends UrlShortenerException {

    public CodeConflictException() {
        super(ErrorCode.SHORT_CODE_CONFLICT);
    }

    public CodeConflictException(String message) {
        super(ErrorCode.SHORT_CODE_CONFLICT, message);
    }
}
