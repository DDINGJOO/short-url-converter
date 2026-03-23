package com.ddingjoo.urlshortener.exception;

public final class UrlGoneException extends UrlShortenerException {

    public UrlGoneException() {
        super(ErrorCode.URL_GONE);
    }
}
