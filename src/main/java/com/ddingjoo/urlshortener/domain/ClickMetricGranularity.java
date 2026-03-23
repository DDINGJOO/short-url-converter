package com.ddingjoo.urlshortener.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public enum ClickMetricGranularity {
	HOUR,
	DAY;
	
	public static ClickMetricGranularity from(String value) {
		return ClickMetricGranularity.valueOf(value.toUpperCase());
	}
	
	public OffsetDateTime normalize(OffsetDateTime value) {
		OffsetDateTime utcValue = value.withOffsetSameInstant(ZoneOffset.UTC);
		return switch (this) {
			case HOUR -> utcValue.truncatedTo(ChronoUnit.HOURS);
			case DAY -> utcValue.truncatedTo(ChronoUnit.DAYS);
		};
	}
}

