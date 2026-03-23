package com.ddingjoo.urlshortener.exception;

public final class ConcurrentOperationException extends UrlShortenerException {

    public ConcurrentOperationException() {
        super(ErrorCode.CONCURRENT_OPERATION);
    }
}

