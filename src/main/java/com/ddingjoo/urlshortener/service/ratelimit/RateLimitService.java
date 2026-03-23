package com.ddingjoo.urlshortener.service.ratelimit;

public interface RateLimitService {
	
	void validate(String clientKey);
}
