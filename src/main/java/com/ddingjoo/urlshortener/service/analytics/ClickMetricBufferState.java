package com.ddingjoo.urlshortener.service.analytics;

import com.ddingjoo.urlshortener.domain.ClickMetricGranularity;

import java.time.OffsetDateTime;

public record ClickMetricBufferState(
		ClickMetricGranularity granularity,
		OffsetDateTime bucketStart,
		long pendingClicks,
		long processingClicks,
		String processingToken
) {
}
