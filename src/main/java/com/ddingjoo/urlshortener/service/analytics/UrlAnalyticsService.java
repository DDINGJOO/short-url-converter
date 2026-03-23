package com.ddingjoo.urlshortener.service.analytics;

import com.ddingjoo.urlshortener.domain.ClickMetricGranularity;
import com.ddingjoo.urlshortener.dto.url.UrlAnalyticsResponse;
import java.time.OffsetDateTime;

public interface UrlAnalyticsService {

    void recordClick(String shortCode, OffsetDateTime clickedAt);

    UrlAnalyticsResponse getAnalytics(
            String shortCode,
            ClickMetricGranularity granularity,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
