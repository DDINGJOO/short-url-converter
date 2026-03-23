package com.ddingjoo.urlshortener.dto.health;

import java.time.OffsetDateTime;

public record HealthResponse(
		String status,
		OffsetDateTime timestamp
) {
}
