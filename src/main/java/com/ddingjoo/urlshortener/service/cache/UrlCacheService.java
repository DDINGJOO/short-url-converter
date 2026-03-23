package com.ddingjoo.urlshortener.service.cache;

import com.ddingjoo.urlshortener.domain.Url;

import java.util.Optional;

public interface UrlCacheService {
	
	Optional<String> findOriginalUrl(String shortCode);
	
	boolean isGone(String shortCode);
	
	void cacheActive(Url url);
	
	void markGone(String shortCode);
}
