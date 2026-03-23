package com.ddingjoo.urlshortener.service.analytics;

import com.ddingjoo.urlshortener.domain.ClickMetricGranularity;

import java.time.OffsetDateTime;

public record ClickMetricSyncBatch(
		ClickMetricGranularity granularity,
		OffsetDateTime bucketStart,
		String token,
		long clicks
) {
}
