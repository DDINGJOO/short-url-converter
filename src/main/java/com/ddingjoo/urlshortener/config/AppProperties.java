package com.ddingjoo.urlshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String baseUrl,
        String adminApiKey,
        long shortCodeObfuscationKey,
        long rateLimitPerMinute
) {
}
