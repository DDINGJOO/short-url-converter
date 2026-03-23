package com.ddingjoo.urlshortener.service.analytics;

import com.ddingjoo.urlshortener.domain.ClickMetricGranularity;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

public interface UrlAnalyticsBufferService {

    void increment(String shortCode, ClickMetricGranularity granularity, OffsetDateTime bucketStart);

    Set<String> findTrackedShortCodes();

    Set<ClickMetricGranularity> findTrackedGranularities(String shortCode);

    Set<OffsetDateTime> findTrackedBuckets(String shortCode, ClickMetricGranularity granularity);

    ClickMetricSyncBatch reserve(String shortCode, ClickMetricGranularity granularity, OffsetDateTime bucketStart);

    boolean acknowledge(String shortCode, ClickMetricGranularity granularity, OffsetDateTime bucketStart, String token);

    Map<OffsetDateTime, ClickMetricBufferState> getBufferedStates(
            String shortCode,
            ClickMetricGranularity granularity,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
