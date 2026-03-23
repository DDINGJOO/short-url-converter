package com.ddingjoo.urlshortener.exception;

public final class RateLimitExceededException extends UrlShortenerException {

    public RateLimitExceededException() {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
    }
}
