package com.ddingjoo.urlshortener.service.analytics;

import com.ddingjoo.urlshortener.domain.ClickMetricGranularity;
import com.ddingjoo.urlshortener.domain.Url;
import com.ddingjoo.urlshortener.domain.UrlClickMetric;
import com.ddingjoo.urlshortener.dto.url.ClickAnalyticsPointResponse;
import com.ddingjoo.urlshortener.dto.url.UrlAnalyticsResponse;
import com.ddingjoo.urlshortener.exception.InvalidUrlException;
import com.ddingjoo.urlshortener.exception.UrlNotFoundException;
import com.ddingjoo.urlshortener.repository.UrlClickMetricRepository;
import com.ddingjoo.urlshortener.repository.UrlRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultUrlAnalyticsService implements UrlAnalyticsService {

    private final UrlRepository urlRepository;
    private final UrlClickMetricRepository urlClickMetricRepository;
    private final UrlAnalyticsBufferService urlAnalyticsBufferService;

    public DefaultUrlAnalyticsService(
            UrlRepository urlRepository,
            UrlClickMetricRepository urlClickMetricRepository,
            UrlAnalyticsBufferService urlAnalyticsBufferService
    ) {
        this.urlRepository = urlRepository;
        this.urlClickMetricRepository = urlClickMetricRepository;
        this.urlAnalyticsBufferService = urlAnalyticsBufferService;
    }

    @Override
    public void recordClick(String shortCode, OffsetDateTime clickedAt) {
        Stream.of(ClickMetricGranularity.HOUR, ClickMetricGranularity.DAY)
                .forEach(granularity -> urlAnalyticsBufferService.increment(shortCode, granularity, granularity.normalize(clickedAt)));
    }

    @Override
    @Transactional(readOnly = true)
    public UrlAnalyticsResponse getAnalytics(
            String shortCode,
            ClickMetricGranularity granularity,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        if (from.isAfter(to)) {
            throw new InvalidUrlException("from must be before or equal to to");
        }

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(UrlNotFoundException::new);

        OffsetDateTime normalizedFrom = granularity.normalize(from);
        OffsetDateTime normalizedTo = granularity.normalize(to);
        List<UrlClickMetric> persistedMetrics = urlClickMetricRepository
                .findAllByUrlIdAndGranularityAndBucketStartBetweenOrderByBucketStartAsc(
                        url.getId(),
                        granularity,
                        normalizedFrom,
                        normalizedTo
                );
        Map<OffsetDateTime, UrlClickMetric> persistedMetricByBucket = persistedMetrics.stream()
                .collect(java.util.stream.Collectors.toMap(
                        UrlClickMetric::getBucketStart,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<OffsetDateTime, ClickMetricBufferState> bufferedStates =
                urlAnalyticsBufferService.getBufferedStates(shortCode, granularity, normalizedFrom, normalizedTo);
        Map<OffsetDateTime, Long> aggregated = persistedMetricByBucket.values().stream()
                .collect(java.util.stream.Collectors.toMap(
                        UrlClickMetric::getBucketStart,
                        UrlClickMetric::getClickCount,
                        Long::sum,
                        LinkedHashMap::new
                ));

        bufferedStates.forEach((bucketStart, state) -> aggregated.merge(
                bucketStart,
                bufferedClicks(state, persistedMetricByBucket.get(bucketStart)),
                Long::sum
        ));

        List<ClickAnalyticsPointResponse> buckets = aggregated.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ClickAnalyticsPointResponse(entry.getKey(), entry.getValue()))
                .toList();

        return new UrlAnalyticsResponse(
                shortCode,
                granularity.name().toLowerCase(),
                normalizedFrom,
                normalizedTo,
                buckets
        );
    }

    private long bufferedClicks(ClickMetricBufferState state, UrlClickMetric persistedMetric) {
        return java.util.stream.LongStream.of(
                        state.pendingClicks(),
                        shouldCountProcessingClicks(state, persistedMetric) ? state.processingClicks() : 0L
                )
                .sum();
    }

    private boolean shouldCountProcessingClicks(ClickMetricBufferState state, UrlClickMetric persistedMetric) {
        return state.processingClicks() > 0
                && (persistedMetric == null || !Objects.equals(persistedMetric.getLastSyncToken(), state.processingToken()));
    }
}
