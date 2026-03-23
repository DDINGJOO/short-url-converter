package com.ddingjoo.urlshortener.service.url;

import com.ddingjoo.urlshortener.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShortCodeGenerator {
	
	private final Base62Encoder base62Encoder;
	private final AppProperties appProperties;
	
	public String generate(Long id) {
		long obfuscatedId = id ^ appProperties.shortCodeObfuscationKey();
		return base62Encoder.encode(obfuscatedId);
	}
}
