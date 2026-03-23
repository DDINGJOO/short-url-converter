package com.ddingjoo.urlshortener.dto.url;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record UrlStatsResponse(
        @JsonProperty("short_code")
        String shortCode,

        @JsonProperty("original_url")
        String originalUrl,

        @JsonProperty("total_clicks")
        long totalClicks,

        @JsonProperty("created_at")
        OffsetDateTime createdAt,

        @JsonProperty("expires_at")
        OffsetDateTime expiresAt,

        @JsonProperty("is_active")
        boolean isActive
) {
}
