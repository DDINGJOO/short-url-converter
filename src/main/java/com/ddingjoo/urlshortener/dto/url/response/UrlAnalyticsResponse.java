package com.ddingjoo.urlshortener.dto.url.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public record UrlAnalyticsResponse(
		@JsonProperty("short_code")
		String shortCode,
		
		String granularity,
		
		OffsetDateTime from,
		
		OffsetDateTime to,
		
		List<ClickAnalyticsPointResponse> buckets
) {
}
