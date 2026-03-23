package com.ddingjoo.urlshortener.exception;

public final class InvalidShortCodeException extends UrlShortenerException {

    public InvalidShortCodeException(String message) {
        super(ErrorCode.INVALID_SHORT_CODE, message);
    }
}
