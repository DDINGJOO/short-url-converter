package com.ddingjoo.urlshortener.dto.url;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record ClickAnalyticsPointResponse(
        @JsonProperty("bucket_start")
        OffsetDateTime bucketStart,

        @JsonProperty("click_count")
        long clickCount
) {
}
