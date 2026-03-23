package com.ddingjoo.urlshortener.dto.url;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

public record ShortenRequest(
        @JsonProperty("original_url")
        @NotBlank
        String originalUrl,

        @JsonProperty("custom_code")
        String customCode,

        @JsonProperty("expires_at")
        OffsetDateTime expiresAt
) {
}
