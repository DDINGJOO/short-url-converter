package com.ddingjoo.urlshortener.dto.url.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record ShortenResponse(
		@JsonProperty("short_code")
		String shortCode,
		
		@JsonProperty("short_url")
		String shortUrl,
		
		@JsonProperty("original_url")
		String originalUrl,
		
		@JsonProperty("expires_at")
		OffsetDateTime expiresAt,
		
		@JsonProperty("created_at")
		OffsetDateTime createdAt
) {
}
