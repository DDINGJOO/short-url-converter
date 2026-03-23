package com.ddingjoo.urlshortener.exception;

public final class UnauthorizedException extends UrlShortenerException {

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED_API_KEY);
    }
}
