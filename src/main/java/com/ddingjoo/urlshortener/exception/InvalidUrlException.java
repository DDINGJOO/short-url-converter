package com.ddingjoo.urlshortener.exception;

public final class InvalidUrlException extends UrlShortenerException {

    public InvalidUrlException(String message) {
        super(ErrorCode.INVALID_URL, message);
    }
}
