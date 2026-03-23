package com.ddingjoo.urlshortener.exception;

public final class UrlNotFoundException extends UrlShortenerException {

    public UrlNotFoundException() {
        super(ErrorCode.URL_NOT_FOUND);
    }
}
